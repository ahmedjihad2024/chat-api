package com.example.chat.user.repository

import com.example.chat.user.entities.User
import com.example.chat.user.enums.Role
import org.bson.types.ObjectId
import java.time.Instant

interface UserRepositoryCustom {
    fun updateUserById(
        id: ObjectId,
        name: String? = null,
        phone: String? = null,
        email: String? = null,
        hashedPassword: String? = null,
        roles: Set<Role>? = null,
        phoneVerified: Boolean? = null,
        phoneVerifiedAt: Instant? = null,
        followersCountDelta: Int? = null,
        followingCountDelta: Int? = null,
        avatarFilename: String? = null,
        deleted: Boolean? = null,
        deletedAt: Instant? = null,
    ): User?
}
