package com.example.chat.chat.entities

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * A single text message in a [Conversation]. The database is the source of truth: a message is
 * always persisted before any best-effort live push, so nothing is lost when the recipient is
 * offline — they pick it up from the history endpoint.
 *
 * The compound `(conversationId, createdAt desc)` index serves the paged history query.
 * [read] is a simple per-message flag (sufficient for 1:1); marking a thread read flips it on
 * the recipient's messages so a read receipt can be derived later.
 */
@Document(collection = "messages")
@CompoundIndex(name = "conversation_createdAt", def = "{'conversationId': 1, 'createdAt': -1}")
data class Message(
    @Id val id: ObjectId = ObjectId(),
    val conversationId: ObjectId,
    val senderId: ObjectId,
    val text: String,
    val read: Boolean = false,
    val createdAt: Instant = Instant.now(),
)
