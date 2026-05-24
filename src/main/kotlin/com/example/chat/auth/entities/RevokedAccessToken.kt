package com.example.chat.auth.entities

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("revoked_access_tokens")
data class RevokedAccessToken(
    @Id val jti: String,
    @Indexed(expireAfter = "0s")
    val expiresAt: Instant,
)
