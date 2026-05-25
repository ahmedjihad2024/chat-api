package com.example.chat.user.entities

import com.example.chat.user.enums.Role
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("users")
data class User(
    val name: String,
    // Primary login identifier in E.164 format (e.g. "+201234567890"); unique and verified.
    @Indexed(unique = true) val phone: String,
    // Optional, unverified contact email. Sparse-unique: two accounts cannot share one, but null is allowed.
    @Indexed(unique = true, sparse = true) val email: String? = null,
    val hashedPassword: String?,
    val roles: Set<Role> = setOf(Role.USER),
    val phoneVerified: Boolean = false,
    val phoneVerifiedAt: Instant? = null,
    // On-disk filename of the profile picture (e.g. "<uuid>.png"); null when the user has no avatar.
    val avatarFilename: String? = null,
    @Id val id: ObjectId = ObjectId()
)
