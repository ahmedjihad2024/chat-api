package com.example.chat.chat

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks which conversation each WebSocket *subscription* is for, so the send path can tell whether
 * a recipient is actively viewing a thread (→ deliver full + mark read) rather than merely online
 * (→ inbox metadata only, stays unread).
 *
 * Presence is derived from STOMP SUBSCRIBE/UNSUBSCRIBE frames server-side (see
 * [com.example.chat.config.WebSocketPresenceListener]), never from a client-sent message, so a
 * client can't read live without being seen: subscribing to a conversation *is* how it receives it.
 *
 * A user may view from several devices; a thread counts as viewed while any of their subscriptions
 * is open. State is in-memory and per-instance — fine for the single-instance simple broker; a
 * multi-instance deployment would need a shared store.
 */
@Component
class ActiveConversationRegistry {

    private data class View(val userId: String, val conversationId: String)

    /** "sessionId|subscriptionId" -> what that subscription is viewing. */
    private val bySubscription = ConcurrentHashMap<String, View>()

    /** sessionId -> its subscription keys, so a disconnect can drop them all. */
    private val sessionSubs = ConcurrentHashMap<String, MutableSet<String>>()

    /** conversationId -> (userId -> number of that user's subscriptions viewing it). */
    private val viewers = ConcurrentHashMap<String, ConcurrentHashMap<String, Int>>()

    /** A subscription to [conversationId] opened. */
    fun viewing(sessionId: String, subscriptionId: String, userId: String, conversationId: String) {
        val key = key(sessionId, subscriptionId)
        if (bySubscription.putIfAbsent(key, View(userId, conversationId)) != null) return
        sessionSubs.computeIfAbsent(sessionId) { ConcurrentHashMap.newKeySet() }.add(key)
        addViewer(userId, conversationId)
    }

    /** A single subscription was cancelled (the client left the thread). */
    fun stopViewing(sessionId: String, subscriptionId: String) {
        val view = bySubscription.remove(key(sessionId, subscriptionId)) ?: return
        sessionSubs[sessionId]?.remove(key(sessionId, subscriptionId))
        removeViewer(view.userId, view.conversationId)
    }

    /** The whole session went away (disconnect): drop every subscription it held. */
    fun sessionGone(sessionId: String) {
        val keys = sessionSubs.remove(sessionId) ?: return
        keys.forEach { key ->
            val view = bySubscription.remove(key) ?: return@forEach
            removeViewer(view.userId, view.conversationId)
        }
    }

    /** True if any of [userId]'s subscriptions currently has [conversationId] open. */
    fun isViewing(userId: String, conversationId: String): Boolean =
        viewers[conversationId]?.containsKey(userId) == true

    private fun key(sessionId: String, subscriptionId: String) = "$sessionId|$subscriptionId"

    private fun addViewer(userId: String, conversationId: String) {
        viewers.computeIfAbsent(conversationId) { ConcurrentHashMap() }.merge(userId, 1, Int::plus)
    }

    private fun removeViewer(userId: String, conversationId: String) {
        viewers.computeIfPresent(conversationId) { _, users ->
            users.merge(userId, -1) { current, delta -> (current + delta).takeIf { it > 0 } }
            if (users.isEmpty()) null else users
        }
    }
}
