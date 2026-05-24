package com.example.chat.security.ratelimit

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("app.rate-limit")
data class RateLimitProperties(
    val enabled: Boolean = true,
    val login: Rule = Rule(capacity = 5, refill = Duration.ofMinutes(1)),
    val register: Rule = Rule(capacity = 5, refill = Duration.ofHours(1)),
    val refresh: Rule = Rule(capacity = 30, refill = Duration.ofMinutes(1)),
    val authenticated: Rule = Rule(capacity = 60, refill = Duration.ofMinutes(1)),
) {
    data class Rule(
        val capacity: Long,
        val refill: Duration,
    )
}
