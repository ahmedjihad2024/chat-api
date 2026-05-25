package com.example.chat.auth

import com.example.chat.auth.dto.AuthResponse
import com.example.chat.auth.passwordReset.dto.ForgotPasswordRequest
import com.example.chat.auth.dto.LoginRequest
import com.example.chat.auth.dto.RefreshRequest
import com.example.chat.auth.dto.RegisterRequest
import com.example.chat.auth.dto.ResendVerificationRequest
import com.example.chat.auth.passwordReset.dto.ResetPasswordRequest
import com.example.chat.auth.dto.TokenResponse
import com.example.chat.auth.dto.VerifyPhoneRequest
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
        ApiResponse.ok(authService.register(body.name, body.phone, body.password, body.email))

    @PostMapping("/login")
    fun login(@Valid @RequestBody body: LoginRequest): ApiResponse<AuthResponse> =
        ApiResponse.ok(authService.login(body.phone, body.password))

    @PostMapping("/verify-phone")
    fun verifyPhone(@Valid @RequestBody body: VerifyPhoneRequest): ApiResponse<AuthResponse.Authenticated> =
        ApiResponse.ok(authService.verifyPhone(body.phone, body.code))

    @PostMapping("/verify-phone/resend")
    fun resendVerification(@Valid @RequestBody body: ResendVerificationRequest): ApiResponse<AuthResponse.VerificationRequired> =
        ApiResponse.ok(authService.resendVerificationCode(body.phone))

    @PostMapping("/password-reset/request")
    fun forgotPassword(@Valid @RequestBody body: ForgotPasswordRequest): ApiResponse<AuthResponse.VerificationRequired> =
        ApiResponse.ok(authService.forgotPassword(body.phone))

    @PostMapping("/password-reset/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun verifyResetCode(@Valid @RequestBody body: VerifyResetCodeRequest) {
        authService.verifyResetCode(body.phone, body.code)
    }

    @PostMapping("/password-reset/confirm")
    fun resetPassword(@Valid @RequestBody body: ResetPasswordRequest): ApiResponse<AuthResponse.Authenticated> =
        ApiResponse.ok(authService.resetPassword(body.phone, body.code, body.newPassword))

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
