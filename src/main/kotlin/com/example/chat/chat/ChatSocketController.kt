package com.example.chat.chat

import com.example.chat.chat.dto.CreateMessageRequest
import com.example.chat.chat.dto.ReadReceipt
import com.example.chat.common.dto.ApiResponse
import com.example.chat.common.exception.ApiException
import com.example.chat.common.extentions.tr
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.stereotype.Controller
import java.security.Principal

/**
 * The live send path. A client sends a [CreateMessageRequest] to `/app/chat.send`; we persist via
 * [ChatService.sendMessage] (source of truth), then fan the result out best-effort:
 * - the full message to both participants' open views (`/user/queue/conv/{id}`) — delivered only to
 *   whoever currently has the thread open;
 * - an [com.example.chat.chat.dto.InboxEvent] to both inboxes (`/user/queue/inbox`) so the
 *   conversation list updates without opening the thread;
 * - if the recipient already had the thread open, the message was delivered already-read, so a
 *   [ReadReceipt] goes back to the sender.
 *
 * If a participant is offline the pushes go nowhere — the message is persisted and picked up from
 * history. Presence (who is "viewing") is tracked from SUBSCRIBE frames, not here; see
 * [com.example.chat.config.WebSocketPresenceListener].
 */
@Controller
class ChatSocketController(
    private val chatService: ChatService,
    private val chatNotifier: ChatNotifier,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @MessageMapping("chat.send")
    fun send(@Valid @Payload request: CreateMessageRequest, principal: Principal) {
        val senderId = principal.name
        val sent = chatService.sendMessage(senderId, request.recipientId, request.text)
        val recipientId = sent.recipientId.toHexString()
        val conversationId = sent.message.conversationId

        chatNotifier.message(recipientId, conversationId, sent.message)
        chatNotifier.message(senderId, conversationId, sent.message)
        chatNotifier.inbox(recipientId, sent.inbox)
        chatNotifier.inbox(senderId, sent.inbox)
        if (sent.recipientWasViewing) {
            chatNotifier.readReceipt(
                senderId,
                ReadReceipt(
                    conversationId,
                    recipientId,
                    sent.message.createdAt
                )
            )
        }
    }

    /**
     * Turns an exception from the send flow into an error frame delivered to the sender's own
     * `/user/queue/errors`, mirroring the REST [ApiResponse.fail] envelope. Without this the
     * STOMP client would just see the connection error with no detail.
     */
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    fun handleException(ex: Exception): ApiResponse<Nothing> {
        log.warn("chat.send failed: {}", ex.message)
        return when (ex) {
            is ApiException -> ApiResponse.fail(ex.errorCode.code, ex.message?.tr() ?: ex.errorCode.code)
            else -> ApiResponse.fail("INTERNAL_ERROR", "error.internal".tr())
        }
    }
}
