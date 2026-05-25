package com.example.chat.user.dto

data class UserResponse(
    val id: String,
    val name: String,
    val phone: String,
    val email: String?,
    val roles: Set<String>,
    val phoneVerified: Boolean,
    val avatar: String? = null,
    val followersCount: Int,
    val followingCount: Int,
)
