package com.example.chat.chat

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks online/offline presence per user across the whole cluster, counting open WebSocket sessions so
 * multi-device works: a user is "online" while *any* of their connections is up, on *any* instance.
 * Connect adds a session id, disconnect removes it; only the real edges matter — 0→1 means the user just
 * came online, →0 means they went offline — so closing one of several devices does not flip them offline.
 *
 * State lives in Redis (`presence:user:{userId}` = a set of that user's open session ids) so every replica
 * sees the same picture. Driven by STOMP CONNECT/DISCONNECT frames server-side (see
 * [com.example.chat.config.socket.WebSocketPresenceListener]).
 *
 * Crash safety: each presence key carries a short TTL ([PRESENCE_TTL]) refreshed every [REFRESH_SECONDS]s
 * for the sessions this instance holds locally. If an instance dies it stops refreshing, so its stale
 * sessions self-expire instead of leaving users stuck "online" forever.
 */
@Component
class PresenceRegistry(
    private val redis: StringRedisTemplate,
) {

    private companion object {
        val PRESENCE_TTL: Duration = Duration.ofSeconds(30)
        const val REFRESH_SECONDS = 10_000L
        fun key(userId: String) = "presence:user:$userId"
    }

    /** sessionId -> userId for sessions held by *this* instance, so the heartbeat can refresh their TTL. */
    private val localSessions = ConcurrentHashMap<String, String>()

    /** A session for [userId] opened. Returns true only if this brought them online (0 → 1). */
    fun connected(userId: String, sessionId: String): Boolean {
        localSessions[sessionId] = userId
        val k = key(userId)
        val added = redis.opsForSet().add(k, sessionId) ?: 0
        redis.expire(k, PRESENCE_TTL)
        val size = redis.opsForSet().size(k) ?: 0
        return added > 0 && size == 1L
    }

    /** A session for [userId] closed. Returns true only if this took them offline (→ 0). */
    fun disconnected(userId: String, sessionId: String): Boolean {
        localSessions.remove(sessionId)
        val k = key(userId)
        val removed = redis.opsForSet().remove(k, sessionId) ?: 0
        val size = redis.opsForSet().size(k) ?: 0
        if (size > 0) redis.expire(k, PRESENCE_TTL)
        return removed > 0 && size == 0L
    }

    /** True if [userId] has at least one open session anywhere in the cluster. */
    fun isOnline(userId: String): Boolean = (redis.opsForSet().size(key(userId)) ?: 0) > 0

    /**
     * Re-assert presence for this instance's live sessions so their keys don't expire under normal
     * operation (refresh interval < TTL). A crashed instance simply stops doing this and its keys lapse.
     */
    @Scheduled(fixedRate = REFRESH_SECONDS)
    fun refreshTtl() {
        localSessions.values.toSet().forEach { userId -> redis.expire(key(userId), PRESENCE_TTL) }
    }
}
