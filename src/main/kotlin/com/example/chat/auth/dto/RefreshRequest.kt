package com.example.chat.auth.dto

import jakarta.validation.constraints.NotBlank

data class RefreshRequest(
    @field:NotBlank(message = "{validation.cannot_be_blank}")
    val refreshToken: String,
)
