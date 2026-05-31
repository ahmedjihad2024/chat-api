package com.example.chat.config

import com.example.chat.chat.live.CHAT_LIVE_CHANNEL
import com.example.chat.chat.live.RedisLiveSubscriber
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
import io.github.bucket4j.distributed.proxy.ClientSideConfig
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import java.time.Duration

/**
 * Wires the two Redis facilities the app can't get from Spring Boot auto-config alone:
 *
 * 1. A bucket4j [LettuceBasedProxyManager] so rate-limit buckets live in Redis (shared across every
 *    replica) instead of in each instance's heap. It uses its own dedicated Lettuce client with a
 *    `String`-key / `byte[]`-value codec, which is what bucket4j stores.
 * 2. A [RedisMessageListenerContainer] subscribing every instance to the live-message channel so the
 *    pub/sub bridge ([RedisLiveSubscriber]) can re-deliver WebSocket pushes to its own local clients.
 *
 * Spring Boot's starter-data-redis already provides the [RedisConnectionFactory] and
 * [org.springframework.data.redis.core.StringRedisTemplate] used by presence / the viewing registry.
 */
@Configuration
class RedisConfig(
    @param:Value($$"${spring.data.redis.url}") private val redisUrl: String,
) {

    /** Dedicated Lettuce client for bucket4j; closed by Spring on shutdown. */
    @Bean(destroyMethod = "shutdown")
    fun bucket4jRedisClient(): RedisClient = RedisClient.create(redisUrl)

    /** String-key / byte[]-value connection — the shape bucket4j's proxy manager requires. */
    @Bean(destroyMethod = "close")
    fun bucket4jConnection(client: RedisClient): StatefulRedisConnection<String, ByteArray> =
        client.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE))

    /**
     * Bucket store. Idle keys expire ~10 min after they'd have fully refilled, bounding Redis memory
     * the way the old Caffeine `maximumSize` + TTL did.
     */
    @Bean
    fun rateLimitProxyManager(
        connection: StatefulRedisConnection<String, ByteArray>,
    ): LettuceBasedProxyManager<String> =
        LettuceBasedProxyManager.builderFor(connection)
            .withClientSideConfig(
                ClientSideConfig.getDefault().withExpirationAfterWriteStrategy(
                    ExpirationAfterWriteStrategy
                        .basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10)),
                ),
            )
            .build()

    /** Subscribes this instance to the live-message channel; the listener delivers to local clients. */
    @Bean
    fun liveMessageListenerContainer(
        connectionFactory: RedisConnectionFactory,
        subscriber: RedisLiveSubscriber,
    ): RedisMessageListenerContainer = RedisMessageListenerContainer().apply {
        setConnectionFactory(connectionFactory)
        addMessageListener(subscriber, ChannelTopic(CHAT_LIVE_CHANNEL))
    }
}
