package com.example.chat.chat.repository

import com.example.chat.chat.entities.Conversation
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.time.Instant

/**
 * Spring Data picks this up by the `<Repository>Impl` naming convention and mixes it into
 * [ConversationRepository]; the [MongoTemplate] is constructor-injected from the context.
 */
class ConversationRepositoryImpl(
    private val mongoTemplate: MongoTemplate,
) : ConversationRepositoryCustom {

    override fun findOrCreate(a: ObjectId, b: ObjectId): Conversation {
        val pairKey = Conversation.pairKeyOf(a, b)
        val update = Update()
            .setOnInsert("pairKey", pairKey)
            .setOnInsert("participantIds", listOf(a, b))
            .setOnInsert("lastMessageAt", Instant.now())
        return mongoTemplate.findAndModify(
            Query(Criteria.where("pairKey").`is`(pairKey)),
            update,
            FindAndModifyOptions().returnNew(true).upsert(true),
            Conversation::class.java,
        )!!
    }

    override fun applySentMessage(conversationId: ObjectId, recipientId: ObjectId, preview: String, at: Instant, incrementUnread: Boolean, previewMessageRead: Boolean) {
        val update = Update()
            .set("lastMessageAt", at)
            .set("lastMessagePreview", preview)
            .set("previewMessageRead", previewMessageRead)
        if (incrementUnread) {
            update.inc("unread.${recipientId.toHexString()}", 1)
        }
        mongoTemplate.updateFirst(
            Query(Criteria.where("_id").`is`(conversationId)),
            update,
            Conversation::class.java,
        )
    }

    override fun resetUnread(conversationId: ObjectId, userId: ObjectId) {
        mongoTemplate.updateFirst(
            Query(Criteria.where("_id").`is`(conversationId)),
            Update().set("unread.${userId.toHexString()}", 0),
            Conversation::class.java,
        )
    }
}
