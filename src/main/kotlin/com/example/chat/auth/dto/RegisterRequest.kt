package com.example.chat.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:Size(min = 2, max = 50, message = "{validation.name.size}")
    val name: String,

    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:Email(message = "{validation.invalid_email}")
    val email: String,

    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:Size(min = 8, max = 100, message = "{validation.password.size}")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+\$",
        message = "{validation.password.pattern}"
    )
    val password: String,
)
