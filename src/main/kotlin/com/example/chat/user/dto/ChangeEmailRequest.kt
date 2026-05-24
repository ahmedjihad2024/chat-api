package com.example.chat.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class ChangeEmailRequest(
    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:Email(message = "{validation.invalid_email}")
    val newEmail: String,
)
