package com.example.chat.auth

import com.example.chat.auth.dto.AuthResponse
import com.example.chat.auth.dto.TokenResponse
import com.example.chat.auth.entities.PhoneVerificationCode
import com.example.chat.auth.passwordReset.PasswordResetCode
import com.example.chat.auth.entities.RefreshToken
import com.example.chat.auth.entities.RevokedAccessToken
import com.example.chat.auth.sms.SmsSender
import com.example.chat.auth.sms.VERIFICATION_CODE_TTL_MINUTES
import com.example.chat.auth.sms.VerificationCodeGenerator
import com.example.chat.auth.repository.PhoneVerificationCodeRepository
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
    private val phoneVerificationCodeRepository: PhoneVerificationCodeRepository,
    private val passwordResetCodeRepository: PasswordResetCodeRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    private val smsSender: SmsSender,
    private val verificationCodeGenerator: VerificationCodeGenerator,
) {

    @Transactional
    fun register(name: String, phone: String, password: String, email: String?): AuthResponse.VerificationRequired {
        val existing = userRepository.findByPhone(phone)
        if (existing != null) {
            if (existing.phoneVerified) {
                throw ApiException.Conflict("error.auth.phone_already_exists")
            }
            phoneVerificationCodeRepository.deleteByUserId(existing.id)
            userRepository.delete(existing)
        }
        if (email != null) {
            val emailOwner = userRepository.findByEmail(email)
            if (emailOwner != null) {
                throw ApiException.Conflict("error.auth.email_already_exists")
            }
        }
        val user = userRepository.save(
            User(
                name = name,
                phone = phone,
                email = email,
                hashedPassword = passwordEncoder.encode(password),
            )
        )
        issueAndSendVerificationCode(user)
        return AuthResponse.VerificationRequired(
            phone = user.phone,
            message = "error.auth.verification_code_sent".tr(),
        )
    }

        @Transactional
        fun login(phone: String, password: String): AuthResponse {
            val user = userRepository.findByPhone(phone)
                ?: throw ApiException.Unauthorized("error.auth.phone_not_found")
        val stored = user.hashedPassword
            ?: throw ApiException.Unauthorized("error.auth.invalid_password")
        if (!passwordEncoder.matches(password, stored)) {
            throw ApiException.Unauthorized("error.auth.invalid_password")
        }
        if (!user.phoneVerified) {
            issueAndSendVerificationCode(user)
            return AuthResponse.VerificationRequired(
                phone = user.phone,
                message = "error.auth.verification_code_sent".tr(),
            )
        }
        val tokens = issueTokens(user)
        return AuthResponse.Authenticated(user.toResponse(), tokens.accessToken, tokens.refreshToken)
    }

    @Transactional
    fun verifyPhone(phone: String, code: String): AuthResponse.Authenticated {
        val user = userRepository.findByPhone(phone)
            ?: throw ApiException.NotFound("error.user.not_found")
        if (user.phoneVerified) {
            throw ApiException.BadRequest("error.auth.phone_already_verified")
        }
        val stored = phoneVerificationCodeRepository.findByUserId(user.id)
            ?: throw ApiException.BadRequest("error.auth.verification_code_invalid")
        if (stored.expiresAt.isBefore(Instant.now())) {
            phoneVerificationCodeRepository.delete(stored)
            throw ApiException.BadRequest("error.auth.verification_code_expired")
        }
        if (stored.code != code.sha256()) {
            throw ApiException.BadRequest("error.auth.verification_code_invalid")
        }
        phoneVerificationCodeRepository.delete(stored)
        val verified = userRepository.save(
            user.copy(phoneVerified = true, phoneVerifiedAt = Instant.now())
        )
        val tokens = issueTokens(verified)
        return AuthResponse.Authenticated(verified.toResponse(), tokens.accessToken, tokens.refreshToken)
    }

    @Transactional
    fun resendVerificationCode(phone: String): AuthResponse.VerificationRequired {
        val user = userRepository.findByPhone(phone)
        // Privacy-preserving: only (re)send when an unverified account actually exists,
        // but always return the same generic message so we never leak which phone numbers
        // are registered or already verified. issueAndSendVerificationCode replaces any
        // existing code, so repeated calls simply refresh and resend.
        if (user != null && !user.phoneVerified) {
            issueAndSendVerificationCode(user)
        }
        return AuthResponse.VerificationRequired(
            phone = phone,
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
    fun forgotPassword(phone: String): AuthResponse.VerificationRequired {
        val user = userRepository.findByPhone(phone)
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
            smsSender.sendVerificationCode(user.phone, plain)
        }
        return AuthResponse.VerificationRequired(
            phone = phone,
            message = "error.auth.password_reset_code_sent".tr(),
        )
    }

    fun verifyResetCode(phone: String, code: String) {
        val user = userRepository.findByPhone(phone)
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
    fun resetPassword(phone: String, code: String, newPassword: String): AuthResponse.Authenticated {
        val user = userRepository.findByPhone(phone)
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
        phoneVerificationCodeRepository.deleteByUserId(user.id)
        val plain = verificationCodeGenerator.generate()
        phoneVerificationCodeRepository.save(
            PhoneVerificationCode(
                userId = user.id,
                code = plain.sha256(),
                expiresAt = Instant.now().plus(VERIFICATION_CODE_TTL_MINUTES, ChronoUnit.MINUTES),
            )
        )
        smsSender.sendVerificationCode(user.phone, plain)
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
