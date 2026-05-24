package com.example.chat.auth.passwordReset.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class ResetPasswordRequest(
    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:Email(message = "{validation.invalid_email}")
    val email: String,

    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:Pattern(regexp = "^\\d{5}$", message = "{validation.verification_code.pattern}")
    val code: String,

    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:Size(min = 8, max = 100, message = "{validation.password.size}")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+\$",
        message = "{validation.password.pattern}"
    )
    val newPassword: String,
)
