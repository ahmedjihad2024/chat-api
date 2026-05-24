package com.example.chat.security.jwt

import com.example.chat.user.enums.Role
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.Claims
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID

@Service
class JwtService(
    @Value("\${jwt.secret}") private val jwtSecret: String
) {

    private val secretKey by lazy {
        Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret))
    }

    private val accessTokenValidityMs  = 15L * 60L * 1000L            // 15 minutes
    val refreshTokenValidityMs: Long
        get() = 30L * 24L * 60L * 60L * 1000L // 30 days

    fun generateAccessToken(userId: String, roles: Set<Role>): String =
        generateToken(userId, "access", accessTokenValidityMs, roles)

    fun generateRefreshToken(userId: String): String =
        generateToken(userId, "refresh", refreshTokenValidityMs, roles = null)

    fun validateAccessToken(token: String): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val tokenType = claims["type"] as? String ?: return false
        return tokenType == "access"
    }

    fun validateRefreshToken(token: String): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val tokenType = claims["type"] as? String ?: return false
        return tokenType == "refresh"
    }

    fun getUserIdFromToken(token: String): String {
        val claims = parseAllClaims(token)
            ?: throw IllegalArgumentException("Invalid token.")
        return claims.subject
    }

    fun getRolesFromToken(token: String): Set<Role> {
        val claims = parseAllClaims(token) ?: return emptySet()
        val raw = claims["roles"] as? List<*> ?: return emptySet()
        return raw.mapNotNull { name -> runCatching { Role.valueOf(name.toString()) }.getOrNull() }.toSet()
    }

    fun getJti(token: String): String {
        val claims = parseAllClaims(token)
            ?: throw IllegalArgumentException("Invalid token.")
        return claims.id ?: throw IllegalArgumentException("Token has no jti.")
    }

    fun getExpiry(token: String): Instant {
        val claims = parseAllClaims(token)
            ?: throw IllegalArgumentException("Invalid token.")
        return claims.expiration.toInstant()
    }

    private fun generateToken(userId: String, type: String, validityMs: Long, roles: Set<Role>?): String {
        val now = Date()
        val expiry = Date(now.time + validityMs)
        val builder = Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(userId)
            .claim("type", type)
            .issuedAt(now)
            .expiration(expiry)
        if (roles != null) {
            builder.claim("roles", roles.map { it.name })
        }
        return builder.signWith(secretKey).compact()
    }

    private fun parseAllClaims(token: String): Claims? {
        val raw = if (token.startsWith("Bearer ")) token.removePrefix("Bearer ") else token
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(raw)
                .payload
        } catch (e: Exception) {
            null
        }
    }
}
