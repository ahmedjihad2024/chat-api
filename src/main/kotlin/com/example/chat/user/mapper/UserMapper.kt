package com.example.chat.user.mapper

import com.example.chat.common.extentions.toAvatarUrl
import com.example.chat.user.dto.UserResponse
import com.example.chat.user.entities.User

fun User.toResponse(): UserResponse = UserResponse(
    id = id.toHexString(),
    name = name,
    phone = phone,
    email = email,
    roles = roles.map { it.name }.toSet(),
    phoneVerified = phoneVerified,
    avatar = avatarFilename.toAvatarUrl(),
    followersCount = followersCount,
    followingCount = followingCount,
)
