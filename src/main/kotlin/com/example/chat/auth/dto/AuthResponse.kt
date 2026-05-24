package com.example.chat.auth.dto

import com.example.chat.user.dto.UserResponse
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = AuthResponse.Authenticated::class, name = "AUTHENTICATED"),
    JsonSubTypes.Type(value = AuthResponse.VerificationRequired::class, name = "VERIFICATION_REQUIRED"),
)
sealed class AuthResponse {

    data class Authenticated(
        val user: UserResponse,
        val accessToken: String,
        val refreshToken: String,
    ) : AuthResponse()

    data class VerificationRequired(
        val email: String,
        val message: String,
    ) : AuthResponse()
}
