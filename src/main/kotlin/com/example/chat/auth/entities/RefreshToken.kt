package com.example.chat.auth.entities

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

// Compound index on (userId, hashedToken) covers findByUserIdAndHashedToken,
// deleteByUserIdAndHashedToken, and deleteAllByUserId (prefix match on userId).
@Document("refresh_tokens")
@CompoundIndex(name = "userId_hashedToken_idx", def = "{'userId': 1, 'hashedToken': 1}")
data class RefreshToken(
    @Id val id: ObjectId = ObjectId(),
    val userId: ObjectId,
    val hashedToken: String,
    val createdAt: Instant = Instant.now(),
    @Indexed(expireAfter = "0s")
    val expiresAt: Instant,
)
