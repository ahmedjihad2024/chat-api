package com.example.chat.config.socket

import com.example.chat.chat.ActiveConversationRegistry
import com.example.chat.chat.ChatService
import com.example.chat.chat.PresenceRegistry
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionConnectedEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent

/**
 * Derives presence from STOMP frames, server-side:
 * - CONNECT → the user's session count goes up; crossing 0→1 notifies their chat partners "online";
 * - SUBSCRIBE to `/user/queue/conv/{id}` → the user is viewing that thread, so it's marked read now
 *   and the [ActiveConversationRegistry] starts treating live messages for it as read on arrival;
 * - UNSUBSCRIBE → they left the thread;
 * - DISCONNECT → every subscription the session held is dropped, and the session count goes down;
 *   crossing →0 (their last device gone) notifies their chat partners "offline".
 *
 * Online/offline counts sessions so a user on several devices stays online until the last one
 * closes. Presence is pushed only to people who have a conversation with the user (their inbox),
 * not broadcast. Because viewing is read from the frame a client must send to *receive* a
 * conversation, a client can't read live messages without being marked present.
 */
@Component
class WebSocketPresenceListener(
    private val activeConversations: ActiveConversationRegistry,
    private val presence: PresenceRegistry,
    private val chatService: ChatService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener
    fun onConnect(event: SessionConnectedEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        val userId = accessor.user?.name ?: return
        val sessionId = accessor.sessionId ?: return
        if (presence.connected(userId, sessionId)) {
            chatService.notifyPresence(userId, online = true)
        }
    }

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
        val userId = event.user?.name ?: return
        if (presence.disconnected(userId, event.sessionId)) {
            chatService.notifyPresence(userId, online = false)
        }
    }

    /** Pulls the conversation id out of a `.../queue/conv/{id}` destination, or null if it isn't one. */
    private fun conversationIdOf(destination: String?): String? {
        val marker = "/queue/conv/"
        val idx = destination?.indexOf(marker) ?: return null
        if (idx < 0) return null
        return destination.substring(idx + marker.length).takeIf { it.isNotBlank() }
    }
}
