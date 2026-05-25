package com.example.chat.follows.dto

import com.example.chat.user.dto.UserResponse
import org.springframework.data.domain.Page

data class ToggleFollowee(
    val following: Boolean,
)

