package com.example.chat.chat

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks which conversation each WebSocket *subscription* is for, so the send path can tell whether a
 * recipient is actively viewing a thread (→ deliver full + mark read) rather than merely online
 * (→ inbox metadata only, stays unread).
 *
 * Presence is derived from STOMP SUBSCRIBE/UNSUBSCRIBE frames server-side (see
 * [com.example.chat.config.socket.WebSocketPresenceListener]), never from a client-sent message, so a
 * client can't read live without being seen: subscribing to a conversation *is* how it receives it.
 *
 * Split between local and shared state, because a socket's whole lifecycle (subscribe → unsubscribe →
 * disconnect) happens on the one instance that holds it, while [isViewing] is asked from any instance:
 * - the subscription bookkeeping ([bySubscription], [sessionSubs]) stays in-memory and per-instance;
 * - only the cluster-visible aggregate ("who is viewing conversation X", `viewing:conv:{id}` in Redis)
 *   is shared, since [com.example.chat.chat.ChatService.sendMessage] reads it for a recipient that may be
 *   connected elsewhere.
 *
 * Crash safety mirrors [PresenceRegistry]: each viewing key carries a TTL refreshed every
 * [REFRESH_SECONDS]s for the conversations this instance currently views, so a dead instance's entries
 * self-expire.
 */
@Component
class ActiveConversationRegistry(
    private val redis: StringRedisTemplate,
) {

    private companion object {
        val VIEWING_TTL: Duration = Duration.ofSeconds(30)
        const val REFRESH_SECONDS = 10_000L
        fun key(conversationId: String) = "viewing:conv:$conversationId"
    }

    private data class View(val userId: String, val conversationId: String)

    /** "sessionId|subscriptionId" -> what that subscription is viewing. */
    private val bySubscription = ConcurrentHashMap<String, View>()

    /** sessionId -> its subscription keys, so a disconnect can drop them all. */
    private val sessionSubs = ConcurrentHashMap<String, MutableSet<String>>()

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

    /** True if any of [userId]'s subscriptions currently has [conversationId] open (cluster-wide). */
    fun isViewing(userId: String, conversationId: String): Boolean =
        redis.opsForHash<String, String>().hasKey(key(conversationId), userId)

    /** Keep this instance's active viewing keys alive (refresh interval < TTL); dead instances lapse. */
    @Scheduled(fixedRate = REFRESH_SECONDS)
    fun refreshTtl() {
        bySubscription.values.map { it.conversationId }.toSet()
            .forEach { conversationId -> redis.expire(key(conversationId), VIEWING_TTL) }
    }

    private fun key(sessionId: String, subscriptionId: String) = "$sessionId|$subscriptionId"

    private fun addViewer(userId: String, conversationId: String) {
        val k = key(conversationId)
        redis.opsForHash<String, String>().increment(k, userId, 1)
        redis.expire(k, VIEWING_TTL)
    }

    private fun removeViewer(userId: String, conversationId: String) {
        val k = key(conversationId)
        val remaining = redis.opsForHash<String, String>().increment(k, userId, -1)
        if (remaining <= 0) redis.opsForHash<String, String>().delete(k, userId)
        if ((redis.opsForHash<String, String>().size(k) ?: 0) > 0) redis.expire(k, VIEWING_TTL)
    }
}
