package com.example.chat.config

import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

// Auto-index creation is disabled in application.yaml (spring.mongodb.auto-index-creation:
// false), so @Indexed annotations on entities are documentation only — they do NOT
// create indexes at runtime. Every index this app relies on must be registered here.
@Component
class IndexInitializer(
    private val mongoTemplate: MongoTemplate,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun ensureIndexes() {
        log.info("Ensuring MongoDB indexes...")

        // users:
        // - phone is the login identifier and must be globally unique (login, register, phone-change check).
        // - email is optional/unverified; sparse-unique so emails stay distinct but null is allowed.
        ensureIndex("users", Index().on("phone", Sort.Direction.ASC).unique())
        ensureIndex("users", Index().on("email", Sort.Direction.ASC).unique().sparse())

        // refresh_tokens:
        // - TTL on expiresAt so expired rows vanish automatically.
        // - Compound (userId, hashedToken) covers both findByUserIdAndHashedToken and
        //   deleteAllByUserId (prefix match on userId).
        ensureIndex("refresh_tokens", Index().on("expiresAt", Sort.Direction.ASC).expire(0, TimeUnit.SECONDS))
        ensureIndex("refresh_tokens", Index().on("userId", Sort.Direction.ASC).on("hashedToken", Sort.Direction.ASC))

        // revoked_access_tokens: TTL only — jti is the @Id, so no extra index needed.
        ensureIndex("revoked_access_tokens", Index().on("expiresAt", Sort.Direction.ASC).expire(0, TimeUnit.SECONDS))

        // phone_verification_codes: one pending row per user, expires automatically.
        ensureIndex("phone_verification_codes", Index().on("userId", Sort.Direction.ASC).unique())
        ensureIndex("phone_verification_codes", Index().on("expiresAt", Sort.Direction.ASC).expire(0, TimeUnit.SECONDS))

        // phone_change_requests: one pending row per user, expires automatically.
        ensureIndex("phone_change_requests", Index().on("userId", Sort.Direction.ASC).unique())
        ensureIndex("phone_change_requests", Index().on("expiresAt", Sort.Direction.ASC).expire(0, TimeUnit.SECONDS))

        // password_reset_codes: one pending row per user, expires automatically.
        ensureIndex("password_reset_codes", Index().on("userId", Sort.Direction.ASC).unique())
        ensureIndex("password_reset_codes", Index().on("expiresAt", Sort.Direction.ASC).expire(0, TimeUnit.SECONDS))

        ensureIndex("follows", Index().on("followerId", Sort.Direction.ASC).on("followeeId", Sort.Direction.ASC).unique())
        ensureIndex("follows", Index().on("followeeId", Sort.Direction.ASC).on("followerId", Sort.Direction.ASC))

        // conversations:
        // - pairKey is unique so there is exactly one 1:1 thread per pair (enables atomic get-or-create upsert).
        // - participantIds + lastMessageAt serves the conversation-list query (my threads, newest first).
        ensureIndex("conversations", Index().on("pairKey", Sort.Direction.ASC).unique())
        ensureIndex("conversations", Index().on("participantIds", Sort.Direction.ASC).on("lastMessageAt", Sort.Direction.DESC))

        // messages: (conversationId, createdAt desc) serves the paged history (newest-first infinite scroll).
        ensureIndex("messages", Index().on("conversationId", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC))

        log.info("MongoDB indexes ensured.")
    }

    /**
     * Creates [index] on [collection], tolerating the case where an index with the same
     * (auto-generated) name already exists with a *different* definition — e.g. after a
     * schema change like email going from unique to unique+sparse. MongoDB rejects an
     * in-place redefinition (IndexKeySpecsConflict / IndexOptionsConflict), so on a
     * conflict we drop the stale index and recreate it. Any other failure is rethrown.
     */
    private fun ensureIndex(collection: String, index: Index) {
        val ops = mongoTemplate.indexOps(collection)
        try {
            ops.createIndex(index)
        } catch (ex: DataAccessException) {
            if (ex.message?.contains("Conflict", ignoreCase = true) != true) throw ex
            val name = defaultIndexName(index)
            log.warn("Index '{}' on '{}' has a conflicting definition; dropping and recreating it.", name, collection)
            ops.dropIndex(name)
            ops.createIndex(index)
        }
    }

    /** Reproduces MongoDB's default index name (e.g. {email:1} -> "email_1", compound -> "a_1_b_-1"). */
    private fun defaultIndexName(index: Index): String =
        index.indexKeys.entries.joinToString("_") { "${it.key}_${it.value}" }
}
