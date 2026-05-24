package com.example.chat.config

import org.slf4j.LoggerFactory
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

        // users: lookup by email is the hot path (login, register, email-change uniqueness check).
        mongoTemplate.indexOps("users").createIndex(
            Index().on("email", Sort.Direction.ASC).unique(),
        )

        // notes: getNotes() pages by ownerId; getOwned() filters by ownerId after _id lookup.
        mongoTemplate.indexOps("notes").createIndex(
            Index().on("ownerId", Sort.Direction.ASC),
        )

        // refresh_tokens:
        // - TTL on expiresAt so expired rows vanish automatically.
        // - Compound (userId, hashedToken) covers both findByUserIdAndHashedToken and
        //   deleteAllByUserId (prefix match on userId).
        mongoTemplate.indexOps("refresh_tokens").createIndex(
            Index().on("expiresAt", Sort.Direction.ASC).expire(0, TimeUnit.SECONDS),
        )
        mongoTemplate.indexOps("refresh_tokens").createIndex(
            Index().on("userId", Sort.Direction.ASC).on("hashedToken", Sort.Direction.ASC),
        )

        // revoked_access_tokens: TTL only — jti is the @Id, so no extra index needed.
        mongoTemplate.indexOps("revoked_access_tokens").createIndex(
            Index().on("expiresAt", Sort.Direction.ASC).expire(0, TimeUnit.SECONDS),
        )

        // email_verification_codes: one pending row per user, expires automatically.
        mongoTemplate.indexOps("email_verification_codes").createIndex(
            Index().on("userId", Sort.Direction.ASC).unique(),
        )
        mongoTemplate.indexOps("email_verification_codes").createIndex(
            Index().on("expiresAt", Sort.Direction.ASC).expire(0, TimeUnit.SECONDS),
        )

        // email_change_requests: one pending row per user, expires automatically.
        mongoTemplate.indexOps("email_change_requests").createIndex(
            Index().on("userId", Sort.Direction.ASC).unique(),
        )
        mongoTemplate.indexOps("email_change_requests").createIndex(
            Index().on("expiresAt", Sort.Direction.ASC).expire(0, TimeUnit.SECONDS),
        )

        // password_reset_codes: one pending row per user, expires automatically.
        mongoTemplate.indexOps("password_reset_codes").createIndex(
            Index().on("userId", Sort.Direction.ASC).unique(),
        )
        mongoTemplate.indexOps("password_reset_codes").createIndex(
            Index().on("expiresAt", Sort.Direction.ASC).expire(0, TimeUnit.SECONDS),
        )

        log.info("MongoDB indexes ensured.")
    }
}
