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
    @Indexed(unique = true) val phone: String,
    @Indexed(unique = true, sparse = true) val email: String? = null,
    val hashedPassword: String?,
    val roles: Set<Role> = setOf(Role.USER),
    val phoneVerified: Boolean = false,
    val phoneVerifiedAt: Instant? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val avatarFilename: String? = null,
    @Id val id: ObjectId = ObjectId()
)
