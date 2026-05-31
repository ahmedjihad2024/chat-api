package com.example.chat.chat.live

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * Publishes a live push to the [CHAT_LIVE_CHANNEL]. Every instance (including this one) receives it via
 * [RedisLiveSubscriber] and delivers to its own locally-connected sessions — so delivery happens
 * exactly once per session no matter which instance the sender and recipient are on, and the push is
 * never sent straight to the local broker here (that would double-deliver on the originating instance).
 */
@Component
class RedisLivePublisher(
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {
    fun publish(userId: String, destination: String, payload: Any) {
        val envelope = LiveEnvelope(
            userId = userId,
            destination = destination,
            payloadType = payload.javaClass.name,
            payload = objectMapper.valueToTree(payload),
        )
        redis.convertAndSend(CHAT_LIVE_CHANNEL, objectMapper.writeValueAsString(envelope))
    }
}
