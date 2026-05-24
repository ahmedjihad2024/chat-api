package com.example.chat.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class ChangePasswordRequest(
    @field:NotBlank(message = "{validation.cannot_be_blank}")
    val currentPassword: String,

    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:Size(min = 8, max = 100, message = "{validation.password.size}")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+\$",
        message = "{validation.password.pattern}",
    )
    val newPassword: String,
)
