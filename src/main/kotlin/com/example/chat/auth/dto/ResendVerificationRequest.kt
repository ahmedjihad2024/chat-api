package com.example.chat.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class ResendVerificationRequest(
    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:Email(message = "{validation.invalid_email}")
    val email: String,
)
