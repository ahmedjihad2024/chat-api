package com.example.chat.chat.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * The payload a client sends over STOMP to `/app/chat.send`. [recipientId] identifies the other
 * user (open chat — any existing user is allowed); the sender is always the authenticated
 * principal, never taken from the payload.
 */
data class CreateMessageRequest(
    @field:NotBlank(message = "{validation.cannot_be_blank}")
    val recipientId: String,

    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:Size(max = 4000, message = "{validation.chat.text.size}")
    val text: String,
)
