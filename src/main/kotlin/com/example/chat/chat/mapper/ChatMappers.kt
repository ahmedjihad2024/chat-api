package com.example.chat.chat.mapper

import com.example.chat.chat.entities.Conversation
import com.example.chat.chat.entities.Message
import com.example.chat.chat.dto.ConversationResponse
import com.example.chat.chat.dto.MessageResponse
import com.example.chat.user.dto.UserResponse

fun Message.toResponse(): MessageResponse = MessageResponse(
    id = id.toHexString(),
    conversationId = conversationId.toHexString(),
    senderId = senderId.toHexString(),
    text = text,
    read = read,
    createdAt = createdAt,
)

/**
 * Renders a conversation for [viewerId]: picks the other participant as [otherUser] and reads
 * the viewer's own unread counter. [otherUser] is supplied pre-loaded so the service can batch
 * the user lookups for a whole page; [online] is the partner's live presence, resolved from the
 * in-memory registry (never persisted), so a freshly-opened app sees who's online right away.
 */
fun Conversation.toResponse(viewerId: String, otherUser: UserResponse, online: Boolean = false): ConversationResponse =
    ConversationResponse(
        id = id.toHexString(),
        otherUser = otherUser,
        lastMessageAt = lastMessageAt,
        lastMessagePreview = lastMessagePreview,
        unread = unread[viewerId] ?: 0,
        online = online,
    )
