package com.example.chat.chat.repository

import com.example.chat.chat.entities.Message
import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface MessageRepository : MongoRepository<Message, ObjectId>, MessageRepositoryCustom {

    /** History for a thread, paged. Pass a `createdAt` desc Pageable for newest-first infinite scroll. */
    fun findByConversationId(conversationId: ObjectId, pageable: Pageable): Page<Message>
}
