package com.example.chat.auth

import com.example.chat.auth.dto.AuthResponse
import com.example.chat.auth.dto.TokenResponse
import com.example.chat.auth.entities.EmailVerificationCode
import com.example.chat.auth.passwordReset.PasswordResetCode
import com.example.chat.auth.entities.RefreshToken
import com.example.chat.auth.entities.RevokedAccessToken
import com.example.chat.auth.mail.Mailer
import com.example.chat.auth.mail.VERIFICATION_CODE_TTL_MINUTES
import com.example.chat.auth.mail.VerificationCodeGenerator
import com.example.chat.auth.repository.EmailVerificationCodeRepository
import com.example.chat.auth.passwordReset.PasswordResetCodeRepository
import com.example.chat.auth.repository.RefreshTokenRepository
import com.example.chat.auth.repository.RevokedAccessTokenRepository
import com.example.chat.common.exception.ApiException
import com.example.chat.common.extentions.sha256
import com.example.chat.common.extentions.tr
import com.example.chat.security.jwt.JwtService
import com.example.chat.user.entities.User
import com.example.chat.user.repository.UserRepository
import com.example.chat.user.mapper.toResponse
import org.bson.types.ObjectId
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val revokedAccessTokenRepository: RevokedAccessTokenRepository,
    private val emailVerificationCodeRepository: EmailVerificationCodeRepository,
    private val passwordResetCodeRepository: PasswordResetCodeRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    private val mailer: Mailer,
    private val verificationCodeGenerator: VerificationCodeGenerator,
) {

    @Transactional
    fun register(name: String, email: String, password: String): AuthResponse.VerificationRequired {
        val existing = userRepository.findByEmail(email)
        if (existing != null) {
            if (existing.emailVerified) {
                throw ApiException.Conflict("error.auth.email_already_exists")
            }
            emailVerificationCodeRepository.deleteByUserId(existing.id)
            userRepository.delete(existing)
        }
        val user = userRepository.save(
            User(
                name = name,
                email = email,
                hashedPassword = passwordEncoder.encode(password),
            )
        )
        issueAndSendVerificationCode(user)
        return AuthResponse.VerificationRequired(
            email = user.email,
            message = "error.auth.verification_code_sent".tr(),
        )
    }

    @Transactional
    fun login(email: String, password: String): AuthResponse {
        val user = userRepository.findByEmail(email)
            ?: throw ApiException.Unauthorized("error.auth.invalid_email")
        val stored = user.hashedPassword
            ?: throw ApiException.Unauthorized("error.auth.invalid_email")
        if (!passwordEncoder.matches(password, stored)) {
            throw ApiException.Unauthorized("error.auth.invalid_password")
        }
        if (!user.emailVerified) {
            issueAndSendVerificationCode(user)
            return AuthResponse.VerificationRequired(
                email = user.email,
                message = "error.auth.verification_code_sent".tr(),
            )
        }
        val tokens = issueTokens(user)
        return AuthResponse.Authenticated(user.toResponse(), tokens.accessToken, tokens.refreshToken)
    }

    @Transactional
    fun verifyEmail(email: String, code: String): AuthResponse.Authenticated {
        val user = userRepository.findByEmail(email)
            ?: throw ApiException.NotFound("error.user.not_found")
        if (user.emailVerified) {
            throw ApiException.BadRequest("error.auth.email_already_verified")
        }
        val stored = emailVerificationCodeRepository.findByUserId(user.id)
            ?: throw ApiException.BadRequest("error.auth.verification_code_invalid")
        if (stored.expiresAt.isBefore(Instant.now())) {
            emailVerificationCodeRepository.delete(stored)
            throw ApiException.BadRequest("error.auth.verification_code_expired")
        }
        if (stored.code != code.sha256()) {
            throw ApiException.BadRequest("error.auth.verification_code_invalid")
        }
        emailVerificationCodeRepository.delete(stored)
        val verified = userRepository.save(
            user.copy(emailVerified = true, emailVerifiedAt = Instant.now())
        )
        val tokens = issueTokens(verified)
        return AuthResponse.Authenticated(verified.toResponse(), tokens.accessToken, tokens.refreshToken)
    }

    @Transactional
    fun resendVerificationCode(email: String): AuthResponse.VerificationRequired {
        val user = userRepository.findByEmail(email)
        // Privacy-preserving: only (re)send when an unverified account actually exists,
        // but always return the same generic message so we never leak which emails are
        // registered or already verified. issueAndSendVerificationCode replaces any
        // existing code, so repeated calls simply refresh and resend.
        if (user != null && !user.emailVerified) {
            issueAndSendVerificationCode(user)
        }
        return AuthResponse.VerificationRequired(
            email = email,
            message = "error.auth.verification_code_sent".tr(),
        )
    }

    @Transactional
    fun refresh(refreshToken: String): TokenResponse {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw ApiException.Unauthorized("error.auth.invalid_refresh_token")
        }
        val userId = ObjectId(jwtService.getUserIdFromToken(refreshToken))
        val hashed = refreshToken.sha256()

        val stored = refreshTokenRepository.findByUserIdAndHashedToken(userId, hashed)
            ?: throw ApiException.Unauthorized("error.auth.refresh_token_revoked")

        refreshTokenRepository.delete(stored)

        val user = userRepository.findById(userId).orElseThrow {
            ApiException.Unauthorized("error.auth.refresh_token_revoked")
        }
        return issueTokens(user)
    }

    fun logout(refreshToken: String, accessToken: String?) {
        if (jwtService.validateRefreshToken(refreshToken)) {
            val userId = ObjectId(jwtService.getUserIdFromToken(refreshToken))
            refreshTokenRepository.deleteByUserIdAndHashedToken(userId, refreshToken.sha256())
        }
        if (accessToken != null && jwtService.validateAccessToken(accessToken)) {
            revokedAccessTokenRepository.save(
                RevokedAccessToken(
                    jti = jwtService.getJti(accessToken),
                    expiresAt = jwtService.getExpiry(accessToken),
                )
            )
        }
    }

    @Transactional
    fun forgotPassword(email: String): AuthResponse.VerificationRequired {
        val user = userRepository.findByEmail(email)
        if (user != null) {
            passwordResetCodeRepository.deleteByUserId(user.id)
            val plain = verificationCodeGenerator.generate()
            passwordResetCodeRepository.save(
                PasswordResetCode(
                    userId = user.id,
                    code = plain.sha256(),
                    expiresAt = Instant.now().plus(VERIFICATION_CODE_TTL_MINUTES, ChronoUnit.MINUTES),
                )
            )
            mailer.sendVerificationCode(user.email, plain)
        }
        return AuthResponse.VerificationRequired(
            email = email,
            message = "error.auth.password_reset_code_sent".tr(),
        )
    }

    fun verifyResetCode(email: String, code: String) {
        val user = userRepository.findByEmail(email)
            ?: throw ApiException.BadRequest("error.auth.password_reset_code_invalid")
        val stored = passwordResetCodeRepository.findByUserId(user.id)
            ?: throw ApiException.BadRequest("error.auth.password_reset_code_invalid")
        if (stored.expiresAt.isBefore(Instant.now())) {
            throw ApiException.BadRequest("error.auth.password_reset_code_expired")
        }
        if (stored.code != code.sha256()) {
            throw ApiException.BadRequest("error.auth.password_reset_code_invalid")
        }
    }

    @Transactional
    fun resetPassword(email: String, code: String, newPassword: String): AuthResponse.Authenticated {
        val user = userRepository.findByEmail(email)
            ?: throw ApiException.BadRequest("error.auth.password_reset_code_invalid")
        val stored = passwordResetCodeRepository.findByUserId(user.id)
            ?: throw ApiException.BadRequest("error.auth.password_reset_code_invalid")
        if (stored.expiresAt.isBefore(Instant.now())) {
            passwordResetCodeRepository.delete(stored)
            throw ApiException.BadRequest("error.auth.password_reset_code_expired")
        }
        if (stored.code != code.sha256()) {
            throw ApiException.BadRequest("error.auth.password_reset_code_invalid")
        }
        val updated = userRepository.save(
            user.copy(hashedPassword = passwordEncoder.encode(newPassword)),
        )
        passwordResetCodeRepository.delete(stored)
        return reissueAfterPasswordChange(updated)
    }

    @Transactional
    fun reissueAfterPasswordChange(user: User): AuthResponse.Authenticated {
        refreshTokenRepository.deleteAllByUserId(user.id)
        val tokens = issueTokens(user)
        return AuthResponse.Authenticated(user.toResponse(), tokens.accessToken, tokens.refreshToken)
    }

    private fun issueAndSendVerificationCode(user: User) {
        emailVerificationCodeRepository.deleteByUserId(user.id)
        val plain = verificationCodeGenerator.generate()
        emailVerificationCodeRepository.save(
            EmailVerificationCode(
                userId = user.id,
                code = plain.sha256(),
                expiresAt = Instant.now().plus(VERIFICATION_CODE_TTL_MINUTES, ChronoUnit.MINUTES),
            )
        )
        mailer.sendVerificationCode(user.email, plain)
    }

    private fun issueTokens(user: User): TokenResponse {
        val accessToken = jwtService.generateAccessToken(user.id.toHexString(), user.roles)
        val refreshToken = jwtService.generateRefreshToken(user.id.toHexString())

        refreshTokenRepository.save(
            RefreshToken(
                userId = user.id,
                hashedToken = refreshToken.sha256(),
                expiresAt = Instant.now().plus(jwtService.refreshTokenValidityMs, ChronoUnit.MILLIS),
            )
        )
        return TokenResponse(accessToken, refreshToken)
    }

}
