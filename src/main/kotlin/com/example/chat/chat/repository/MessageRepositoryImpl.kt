package com.example.chat.chat.repository

import com.example.chat.chat.entities.Message
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

/**
 * Spring Data picks this up by the `<Repository>Impl` naming convention and mixes it into
 * [MessageRepository]; the [MongoTemplate] is constructor-injected from the context.
 */
class MessageRepositoryImpl(
    private val mongoTemplate: MongoTemplate,
) : MessageRepositoryCustom {

    override fun markIncomingRead(conversationId: ObjectId, reader: ObjectId): Long =
        mongoTemplate.updateMulti(
            Query(
                Criteria.where("conversationId").`is`(conversationId)
                    .and("senderId").ne(reader)
                    .and("read").`is`(false),
            ),
            Update().set("read", true),
            Message::class.java,
        ).modifiedCount
}
