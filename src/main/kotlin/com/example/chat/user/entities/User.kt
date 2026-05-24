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
    @Indexed(unique = true) val email: String,
    val hashedPassword: String?,
    val roles: Set<Role> = setOf(Role.USER),
    val emailVerified: Boolean = false,
    val emailVerifiedAt: Instant? = null,
    // On-disk filename of the profile picture (e.g. "<uuid>.png"); null when the user has no avatar.
    val avatarFilename: String? = null,
    @Id val id: ObjectId = ObjectId()
)
