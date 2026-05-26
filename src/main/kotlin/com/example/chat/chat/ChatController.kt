package com.example.chat.chat

import com.example.chat.chat.dto.ConversationReadStatus
import com.example.chat.chat.dto.ConversationResponse
import com.example.chat.chat.dto.MessageResponse
import com.example.chat.common.dto.ApiResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST surface for chat: open a thread, list threads, read history, and mark read.
 * Sending is done over WebSocket (`/app/chat.send`), not here — see [ChatSocketController].
 * Every endpoint authorizes that the caller is a participant of the thread it touches.
 */
@RestController
@RequestMapping("/api/chats")
class ChatController(
    private val chatService: ChatService,
) {

//    /** Get-or-create the conversation with [userId]. The caller is always the authenticated user. */
//    @PostMapping("/{userId}")
//    fun open(
//        @PathVariable userId: String,
//        @AuthenticationPrincipal currentUserId: String,
//    ): ApiResponse<ConversationResponse> =
//        chatService.openConversation(currentUserId, userId)

    /** My conversations, paged, sorted by most recent activity. */
    @GetMapping
    fun myConversations(
        @AuthenticationPrincipal currentUserId: String,
        @PageableDefault(size = 20, sort = ["lastMessageAt"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): ApiResponse<List<ConversationResponse>> =
        chatService.listConversations(currentUserId, pageable)

    /** Message history (infinite scroll); also delivers messages received while offline. */
    @GetMapping("/{conversationId}/messages")
    fun history(
        @PathVariable conversationId: String,
        @AuthenticationPrincipal currentUserId: String,
        @PageableDefault(size = 30, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): ApiResponse<List<MessageResponse>> =
        chatService.history(currentUserId, conversationId, pageable)

//    /** Read state of the thread: each side's unread count (0 = that side has read everything). */
//    @GetMapping("/{conversationId}/read")
//    fun readStatus(
//        @PathVariable conversationId: String,
//        @AuthenticationPrincipal currentUserId: String,
//    ): ApiResponse<ConversationReadStatus> =
//        chatService.readStatus(currentUserId, conversationId)

//    /** Manually mark the thread read: clears the caller's unread counter. Reading history does this too. */
//    @PostMapping("/{conversationId}/read")
//    fun markRead(
//        @PathVariable conversationId: String,
//        @AuthenticationPrincipal currentUserId: String,
//    ): ApiResponse<Unit> =
//        chatService.markRead(currentUserId, conversationId)
}
