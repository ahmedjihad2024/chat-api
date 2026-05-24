package com.example.chat.auth.entities

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("email_verification_codes")
data class EmailVerificationCode(
    @Id val id: ObjectId = ObjectId(),
    @Indexed(unique = true) val userId: ObjectId,
    val code: String,
    val createdAt: Instant = Instant.now(),
    @Indexed(expireAfter = "0s")
    val expiresAt: Instant,
)
