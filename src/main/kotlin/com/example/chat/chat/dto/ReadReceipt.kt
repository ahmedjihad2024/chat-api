package com.example.chat.chat.dto

import java.time.Instant

/**
 * Pushed over WebSocket to a message's author when the other participant reads the thread, so the
 * sender's UI can flip its checkmarks live. Conversation-level: every message the author sent in
 * [conversationId] with `createdAt <= readAt` is now read.
 */
data class ReadReceipt(
    val conversationId: String,
    val readerId: String,
    val readAt: Instant,
)
