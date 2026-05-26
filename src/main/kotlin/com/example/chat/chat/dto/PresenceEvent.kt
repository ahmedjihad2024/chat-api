package com.example.chat.chat.dto

import java.time.Instant

/**
 * Pushed to `/user/queue/inbox` of each person who has a conversation with [userId] when that user
 * crosses the online/offline edge, so their chat list can flip the partner's presence dot live.
 * Sent only to chat partners (not broadcast), and only on the real transitions (first connection up
 * / last connection gone) — not on every device a user opens or closes.
 */
data class PresenceEvent(
    val userId: String,
    val online: Boolean,
    val at: Instant,
)
