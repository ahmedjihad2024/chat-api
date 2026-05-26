package com.example.chat.chat.repository

import com.example.chat.chat.entities.Conversation
import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface ConversationRepository : MongoRepository<Conversation, ObjectId>, ConversationRepositoryCustom {

    /** My conversations, paged and (via the caller's Pageable) sorted by `lastMessageAt` desc. */
    fun findByParticipantIds(participantId: ObjectId, pageable: Pageable): Page<Conversation>

    /** All of a user's conversations (unpaged) — used to fan presence out to their chat partners. */
    fun findByParticipantIds(participantId: ObjectId): List<Conversation>

    fun findByPairKey(pairKey: String): Conversation?
}
