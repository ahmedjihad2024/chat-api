package com.example.chat.chat.dto

import com.example.chat.user.dto.UserResponse
import java.time.Instant

/**
 * One row in the conversation list, from the caller's point of view. [otherUser] is the single
 * other participant (1:1), [unread] is the caller's own unread count for this thread.
 */
data class ConversationResponse(
    val id: String,
    val otherUser: UserResponse,
    val lastMessageAt: Instant,
    val lastMessagePreview: String?,
    val unread: Int,
    val online: Boolean,
)
