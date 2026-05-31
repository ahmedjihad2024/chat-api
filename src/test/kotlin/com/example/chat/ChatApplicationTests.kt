package com.example.chat

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import redis.embedded.RedisServer
import java.net.ServerSocket

@SpringBootTest
class ChatApplicationTests {

    companion object {
        // Pick a free port and start an embedded Redis at class load — before the Spring context is
        // built — so the bucket4j connection and the live-message pub/sub listener can connect.
        private val redisPort = ServerSocket(0).use { it.localPort }
        private val redisServer = RedisServer(redisPort).also { it.start() }

        @JvmStatic
        @DynamicPropertySource
        fun redisProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.url") { "redis://localhost:$redisPort" }
        }

        @JvmStatic
        @AfterAll
        fun stopRedis() {
            redisServer.stop()
        }
    }

    @Test
    fun contextLoads() {
    }
}
