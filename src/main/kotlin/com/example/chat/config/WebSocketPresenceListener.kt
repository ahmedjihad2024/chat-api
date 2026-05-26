package com.example.chat.config

import com.example.chat.chat.ActiveConversationRegistry
import com.example.chat.chat.ChatService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent

/**
 * Derives "who is viewing which conversation" from STOMP frames, server-side:
 * - SUBSCRIBE to `/user/queue/conv/{id}` → the user is viewing that thread, so it's marked read now
 *   and the [ActiveConversationRegistry] starts treating live messages for it as read on arrival;
 * - UNSUBSCRIBE → they left the thread;
 * - DISCONNECT → every subscription the session held is dropped.
 *
 * Because this reads the frame the client must send to *receive* the conversation, a client can't
 * read live messages without being marked present.
 */
@Component
class WebSocketPresenceListener(
    private val activeConversations: ActiveConversationRegistry,
    private val chatService: ChatService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener
    fun onSubscribe(event: SessionSubscribeEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        val conversationId = conversationIdOf(accessor.destination) ?: return
        val userId = accessor.user?.name ?: return
        val sessionId = accessor.sessionId ?: return
        val subscriptionId = accessor.subscriptionId ?: return

        activeConversations.viewing(sessionId, subscriptionId, userId, conversationId)
        // Mark the thread read now (participant check lives in the service); a non-participant or a
        // bad id simply fails here and pushes nothing.
        runCatching { chatService.markRead(userId, conversationId) }
            .onFailure { log.debug("mark-read on subscribe skipped: {}", it.message) }
    }

    @EventListener
    fun onUnsubscribe(event: SessionUnsubscribeEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = accessor.sessionId ?: return
        val subscriptionId = accessor.subscriptionId ?: return
        activeConversations.stopViewing(sessionId, subscriptionId)
    }

    @EventListener
    fun onDisconnect(event: SessionDisconnectEvent) {
        activeConversations.sessionGone(event.sessionId)
    }

    /** Pulls the conversation id out of a `.../queue/conv/{id}` destination, or null if it isn't one. */
    private fun conversationIdOf(destination: String?): String? {
        val marker = "/queue/conv/"
        val idx = destination?.indexOf(marker) ?: return null
        if (idx < 0) return null
        return destination.substring(idx + marker.length).takeIf { it.isNotBlank() }
    }
}
