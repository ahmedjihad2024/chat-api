package com.example.chat.auth

import com.example.chat.auth.dto.AuthResponse
import com.example.chat.auth.passwordReset.dto.ForgotPasswordRequest
import com.example.chat.auth.dto.LoginRequest
import com.example.chat.auth.dto.RefreshRequest
import com.example.chat.auth.dto.RegisterRequest
import com.example.chat.auth.dto.ResendVerificationRequest
import com.example.chat.auth.passwordReset.dto.ResetPasswordRequest
import com.example.chat.auth.dto.TokenResponse
import com.example.chat.auth.dto.VerifyEmailRequest
import com.example.chat.auth.passwordReset.dto.VerifyResetCodeRequest
import com.example.chat.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/auth")
class AuthController(
    private val authService: AuthService,
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody body: RegisterRequest): ApiResponse<AuthResponse.VerificationRequired> =
        ApiResponse.ok(authService.register(body.name, body.email, body.password))

    @PostMapping("/login")
    fun login(@Valid @RequestBody body: LoginRequest): ApiResponse<AuthResponse> =
        ApiResponse.ok(authService.login(body.email, body.password))

    @PostMapping("/verify-email")
    fun verifyEmail(@Valid @RequestBody body: VerifyEmailRequest): ApiResponse<AuthResponse.Authenticated> =
        ApiResponse.ok(authService.verifyEmail(body.email, body.code))

    @PostMapping("/verify-email/resend")
    fun resendVerification(@Valid @RequestBody body: ResendVerificationRequest): ApiResponse<AuthResponse.VerificationRequired> =
        ApiResponse.ok(authService.resendVerificationCode(body.email))

    @PostMapping("/password-reset/request")
    fun forgotPassword(@Valid @RequestBody body: ForgotPasswordRequest): ApiResponse<AuthResponse.VerificationRequired> =
        ApiResponse.ok(authService.forgotPassword(body.email))

    @PostMapping("/password-reset/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun verifyResetCode(@Valid @RequestBody body: VerifyResetCodeRequest) {
        authService.verifyResetCode(body.email, body.code)
    }

    @PostMapping("/password-reset/confirm")
    fun resetPassword(@Valid @RequestBody body: ResetPasswordRequest): ApiResponse<AuthResponse.Authenticated> =
        ApiResponse.ok(authService.resetPassword(body.email, body.code, body.newPassword))

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody body: RefreshRequest): ApiResponse<TokenResponse> =
        ApiResponse.ok(authService.refresh(body.refreshToken))

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(
        @Valid @RequestBody body: RefreshRequest,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authHeader: String?,
    ) {
        val accessToken = authHeader?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")
        authService.logout(body.refreshToken, accessToken)
    }
}
