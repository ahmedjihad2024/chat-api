package com.example.chat.security.ratelimit

import com.example.chat.common.dto.ApiResponse
import com.example.chat.common.exception.ErrorCode
import com.example.chat.common.extentions.tr
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitFilter(
    private val properties: RateLimitProperties,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    /**
     * In-memory bucket store. Single-instance only — running multiple replicas means
     * each instance enforces limits independently. Swap for a Redis-backed bucket
     * (bucket4j-redis) when running clustered.
     */
    private val buckets = ConcurrentHashMap<String, Bucket>()

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

        val bucket = buckets.computeIfAbsent(resolution.key) { newBucket(resolution.rule) }
        val probe = bucket.tryConsumeAndReturnRemaining(1)

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

    private fun newBucket(rule: RateLimitProperties.Rule): Bucket {
        val limit = Bandwidth.classic(rule.capacity, Refill.intervally(rule.capacity, rule.refill))
        return Bucket.builder().addLimit(limit).build()
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
