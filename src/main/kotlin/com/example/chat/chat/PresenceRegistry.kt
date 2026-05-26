package com.example.chat.chat

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks online/offline presence per user, counting open WebSocket sessions so multi-device works:
 * a user is "online" while *any* of their connections is up. Connect increments, disconnect
 * decrements; only the real edges matter — 0→1 means the user just came online, →0 means they went
 * offline — so closing one of several devices does not flip them offline.
 *
 * Driven by STOMP CONNECT/DISCONNECT frames server-side (see
 * [com.example.chat.config.socket.WebSocketPresenceListener]). State is in-memory and per-instance —
 * fine for the single-instance simple broker; a multi-instance deployment would need a shared store.
 */
@Component
class PresenceRegistry {

    /** userId -> number of its currently open sessions. Absent == offline. */
    private val sessionCounts = ConcurrentHashMap<String, Int>()

    /** A session for [userId] opened. Returns true only if this brought them online (0 → 1). */
    fun connected(userId: String): Boolean {
        var cameOnline = false
        sessionCounts.compute(userId) { _, current ->
            val next = (current ?: 0) + 1
            if (next == 1) cameOnline = true
            next
        }
        return cameOnline
    }

    /** A session for [userId] closed. Returns true only if this took them offline (→ 0). */
    fun disconnected(userId: String): Boolean {
        var wentOffline = false
        sessionCounts.compute(userId) { _, current ->
            val next = (current ?: 0) - 1
            if (next <= 0) {
                wentOffline = (current ?: 0) > 0
                null
            } else {
                next
            }
        }
        return wentOffline
    }

    /** True if [userId] has at least one open session. */
    fun isOnline(userId: String): Boolean = sessionCounts.containsKey(userId)

    /** Snapshot of everyone currently online. */
    fun onlineUsers(): Set<String> = sessionCounts.keys.toSet()
}
