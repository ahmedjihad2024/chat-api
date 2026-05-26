package com.example.chat.chat.entities

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * A 1:1 chat thread between exactly two users.
 *
 * [pairKey] is the two participant ids sorted and joined (`"idA_idB"`) and carries a unique
 * index, so there is exactly one thread per pair. That makes get-or-create a single atomic
 * upsert keyed on [pairKey] — concurrent "open chat with X" calls can never create duplicates.
 *
 * [unread] holds the unread count per participant (keyed by the participant's id hex). It is the
 * source of truth for the conversation-list badge; sending bumps the recipient's entry, and
 * marking the thread read resets the caller's entry to 0.
 */
@Document(collection = "conversations")
data class Conversation(
    @Id val id: ObjectId = ObjectId(),
    val participantIds: List<ObjectId>,
    @Indexed(unique = true) val pairKey: String,
    val lastMessageAt: Instant = Instant.now(),
    val lastMessagePreview: String? = null,
    val previewMessageRead: Boolean = false,
    val unread: Map<String, Int> = emptyMap(),
) {
    companion object {
        /** The canonical, order-independent key for a pair of users. */
        fun pairKeyOf(a: ObjectId, b: ObjectId): String =
            listOf(a.toHexString(), b.toHexString()).sorted().joinToString("_")
    }
}
