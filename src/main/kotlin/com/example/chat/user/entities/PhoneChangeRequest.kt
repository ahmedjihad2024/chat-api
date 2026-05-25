package com.example.chat.user.entities

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("phone_change_requests")
data class PhoneChangeRequest(
    @Id val id: ObjectId = ObjectId(),
    @Indexed(unique = true) val userId: ObjectId,
    val newPhone: String,
    val code: String,
    val createdAt: Instant = Instant.now(),
    @Indexed(expireAfter = "0s")
    val expiresAt: Instant,
)
