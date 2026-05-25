package com.example.chat.user.mapper

import com.example.chat.user.entities.User
import com.example.chat.user.dto.UserResponse

fun User.toResponse(avatar: String? = null): UserResponse = UserResponse(
    id = id.toHexString(),
    name = name,
    phone = phone,
    email = email,
    roles = roles.map { it.name }.toSet(),
    phoneVerified = phoneVerified,
    avatar = avatar,
)
