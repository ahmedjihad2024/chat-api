package com.example.chat.chat.repository

import com.example.chat.chat.entities.Conversation
import org.bson.types.ObjectId
import java.time.Instant

/**
 * Conversation data-access that can't be expressed as a derived query: the atomic find-or-create
 * upsert and the per-participant `unread` map updates (whose field path is built from a user id).
 * Implemented in [ConversationRepositoryImpl] and mixed into [ConversationRepository].
 */
interface ConversationRepositoryCustom {

    /** Atomic find-or-insert of the 1:1 thread, keyed on the unique pairKey. */
    fun findOrCreate(a: ObjectId, b: ObjectId): Conversation

    /**
     * Set the [preview] + `lastMessageAt`; increment the recipient's unread counter only when
     * [incrementUnread] is true (it is false when the recipient is actively viewing the thread,
     * so the message is delivered already-read).
     */
    fun applySentMessage(conversationId: ObjectId, recipientId: ObjectId, preview: String, at: Instant, incrementUnread: Boolean, previewMessageRead: Boolean)

    /** Reset a participant's unread counter to zero. */
    fun resetUnread(conversationId: ObjectId, userId: ObjectId)
}
