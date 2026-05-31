package com.example.chat.security.ratelimit

import com.example.chat.common.dto.ApiResponse
import com.example.chat.common.exception.ErrorCode
import com.example.chat.common.extentions.tr
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper
import java.util.function.Supplier

@Component
class RateLimitFilter(
    private val properties: RateLimitProperties,
    private val objectMapper: ObjectMapper,
    /**
     * Redis-backed bucket store: every replica shares one bucket per key, so the limit is enforced
     * globally instead of N times. Idle keys expire (configured on the proxy manager) to bound memory.
     */
    private val proxyManager: LettuceBasedProxyManager<String>,
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (!properties.enabled) {
            filterChain.doFilter(request, response)
            return
        }

        val resolution = resolve(request) ?: run {
            filterChain.doFilter(request, response)
            return
        }

        val probe = try {
            val bucket = proxyManager.builder()
                .build(resolution.key) { bucketConfig(resolution.rule) }
            bucket.tryConsumeAndReturnRemaining(1)
        } catch (e: Exception) {
            // Fail-open: a Redis outage must not lock real users out of the whole API. We lose
            // throttling for the duration of the outage, which is the safer trade-off here.
            log.warn("Rate limit check skipped — Redis unavailable: {}", e.message)
            filterChain.doFilter(request, response)
            return
        }

        if (!probe.isConsumed) {
            writeTooManyRequests(response, probe.nanosToWaitForRefill / 1_000_000_000L)
            return
        }

        response.setHeader("X-RateLimit-Remaining", probe.remainingTokens.toString())
        filterChain.doFilter(request, response)
    }

    private fun resolve(request: HttpServletRequest): Resolution? {
        val path = request.requestURI
        val method = request.method
        val ip = clientIp(request)

        return when {
            method == "POST" && path == "/api/auth/login" ->
                Resolution("login:$ip", properties.login)
            method == "POST" && path == "/api/auth/register" ->
                Resolution("register:$ip", properties.register)
            method == "POST" && path == "/api/auth/refresh" ->
                Resolution("refresh:$ip", properties.refresh)
            path.startsWith("/api/") -> {
                val principal = SecurityContextHolder.getContext().authentication?.principal as? String
                val key = if (principal != null) "user:$principal" else "ip:$ip"
                Resolution(key, properties.authenticated)
            }
            else -> null
        }
    }

    private fun bucketConfig(rule: RateLimitProperties.Rule): BucketConfiguration {
        val limit = Bandwidth.builder()
            .capacity(rule.capacity)
            .refillIntervally(rule.capacity, rule.refill)
            .build()
        return BucketConfiguration.builder().addLimit(limit).build()
    }

    private fun clientIp(request: HttpServletRequest): String {
        val forwarded = request.getHeader("X-Forwarded-For")
        if (!forwarded.isNullOrBlank()) return forwarded.split(",").first().trim()
        return request.remoteAddr ?: "unknown"
    }

    private fun writeTooManyRequests(response: HttpServletResponse, retryAfterSeconds: Long) {
        response.status = 429
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        if (retryAfterSeconds > 0) {
            response.setHeader("Retry-After", retryAfterSeconds.toString())
        }
        val body = ApiResponse.fail(ErrorCode.RATE_LIMIT_EXCEEDED.code, "error.rate_limit.exceeded".tr())
        objectMapper.writeValue(response.outputStream, body)
    }

    private data class Resolution(val key: String, val rule: RateLimitProperties.Rule)
}
