package com.example.chat.chat.dto

/**
 * Per-participant read state of a 1:1 thread. Each [SideReadStatus.unread] is that participant's
 * outstanding count: `0` means they have read everything the other side sent.
 */
data class ConversationReadStatus(
    val conversationId: String,
    val sides: List<SideReadStatus>,
)

data class SideReadStatus(
    val userId: String,
    val unread: Int,
)
