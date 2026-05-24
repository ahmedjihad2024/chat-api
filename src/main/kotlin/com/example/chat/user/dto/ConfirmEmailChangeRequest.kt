package com.example.chat.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class ConfirmEmailChangeRequest(
    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:Pattern(regexp = "^\\d{5}$", message = "{validation.verification_code.pattern}")
    val code: String,
)
