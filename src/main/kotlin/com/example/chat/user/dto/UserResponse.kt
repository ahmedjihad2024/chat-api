package com.example.chat.user.dto

data class UserResponse(
    val id: String,
    val name: String,
    val phone: String,
    // Optional, unverified contact email; null when the user hasn't set one.
    val email: String?,
    val roles: Set<String>,
    val phoneVerified: Boolean,
    // Profile picture as a base64 data URI (e.g. "data:image/png;base64,..."), or null when unset.
    val avatar: String? = null,
)
