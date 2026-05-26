package com.example.chat.chat.dto

import java.time.Instant

/**
 * Lightweight "a thread changed" event pushed to `/user/queue/inbox` so the conversation list can
 * update without opening the thread. Carries only a truncated [preview] (never the full text), so
 * receiving it is like seeing a notification preview — it does not count as reading the message.
 */
data class InboxEvent(
    val conversationId: String,
    val senderId: String,
    val preview: String,
    val sentAt: Instant,
    val messageId: String,
    val previewMessageRead: Boolean,
)
