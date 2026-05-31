package com.example.chat.chat.live

import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * Receives every live push published to [CHAT_LIVE_CHANNEL] and hands it to this instance's local STOMP
 * broker. `convertAndSendToUser` only reaches sessions on this instance, so instances that don't hold
 * the target user's session simply deliver nothing — which is exactly the fan-out we want. Best-effort:
 * a malformed or undeliverable message is logged and dropped (the DB remains the source of truth).
 */
@Component
class RedisLiveSubscriber(
    private val messagingTemplate: SimpMessagingTemplate,
    private val objectMapper: ObjectMapper,
) : MessageListener {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun onMessage(message: Message, pattern: ByteArray?) {
        runCatching {
            val envelope = objectMapper.readValue(message.body, LiveEnvelope::class.java)
            val payload = objectMapper.treeToValue(envelope.payload, Class.forName(envelope.payloadType))
            messagingTemplate.convertAndSendToUser(envelope.userId, envelope.destination, payload)
        }.onFailure { log.warn("Dropping live message from Redis: {}", it.message) }
    }
}
