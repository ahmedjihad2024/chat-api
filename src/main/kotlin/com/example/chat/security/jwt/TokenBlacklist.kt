package com.example.chat.security.jwt

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

/**
 * Cluster-wide blacklist of revoked access tokens, keyed by the token's `jti`.
 *
 * On logout (or any revocation) the `jti` is written to Redis under [key] with a TTL equal to the
 * token's *remaining* lifetime, so the entry self-expires exactly when the token would have anyway —
 * no sweep job, no growth beyond the set of still-valid revoked tokens. Every replica reads the same
 * Redis, so a token revoked on one instance is rejected everywhere (see [JwtAuthFilter] and
 * [com.example.chat.config.socket.WebSocketAuthInterceptor]).
 *
 * Replaces the previous MongoDB `revoked_access_tokens` collection: Redis' native per-key TTL is a
 * better fit than a Mongo TTL index, and the lookup is an O(1) key check on the hot auth path.
 */
@Component
class TokenBlacklist(
    private val redis: StringRedisTemplate,
) {

    private companion object {
        fun key(jti: String) = "blacklist:jti:$jti"
    }

    /**
     * Revoke the token with this [jti] until [expiresAt]. No-op if the token has already expired
     * (nothing to block — it would fail validation anyway).
     */
    fun revoke(jti: String, expiresAt: Instant) {
        val ttl = Duration.between(Instant.now(), expiresAt)
        if (ttl.isZero || ttl.isNegative) return
        redis.opsForValue().set(key(jti), "1", ttl)
    }

    /** True if the token with this [jti] has been revoked and is still within its lifetime. */
    fun isRevoked(jti: String): Boolean = redis.hasKey(key(jti))
}
