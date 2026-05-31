package com.example.chat.chat.live

import tools.jackson.databind.JsonNode

/** Redis pub/sub channel every instance publishes live WebSocket pushes to and subscribes to. */
const val CHAT_LIVE_CHANNEL = "chat:live"

/**
 * One live WebSocket push, routed through Redis so it can reach the recipient on whichever instance
 * actually holds their session. [payload] is the original DTO captured as a JSON tree; [payloadType]
 * is its class name so the subscriber can rebuild the exact object and hand it to the STOMP broker —
 * making the JSON the client receives identical to the single-instance path.
 */
data class LiveEnvelope(
    val userId: String,
    val destination: String,
    val payloadType: String,
    val payload: JsonNode,
)
