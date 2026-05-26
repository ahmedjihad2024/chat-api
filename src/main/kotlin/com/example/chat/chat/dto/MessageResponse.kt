package com.example.chat.chat.dto

import java.time.Instant

data class MessageResponse(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val read: Boolean,
    val createdAt: Instant,
)
