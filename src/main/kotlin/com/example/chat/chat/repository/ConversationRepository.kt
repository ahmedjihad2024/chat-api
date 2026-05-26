package com.example.chat.chat.repository

import com.example.chat.chat.entities.Conversation
import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface ConversationRepository : MongoRepository<Conversation, ObjectId>, ConversationRepositoryCustom {

    /** My conversations, paged and (via the caller's Pageable) sorted by `lastMessageAt` desc. */
    fun findByParticipantIds(participantId: ObjectId, pageable: Pageable): Page<Conversation>

    fun findByPairKey(pairKey: String): Conversation?
}
