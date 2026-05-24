package com.example.chat.user

import com.example.chat.auth.AuthService
import com.example.chat.auth.dto.AuthResponse
import com.example.chat.auth.mail.Mailer
import com.example.chat.auth.mail.VERIFICATION_CODE_TTL_MINUTES
import com.example.chat.auth.mail.VerificationCodeGenerator
import com.example.chat.common.extentions.sha256
import com.example.chat.common.exception.ApiException
import com.example.chat.common.extentions.tr
import com.example.chat.user.entities.EmailChangeRequest
import com.example.chat.user.entities.User
import com.example.chat.user.repository.EmailChangeRequestRepository
import com.example.chat.user.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class UserService(
    private val userRepository: UserRepository,
    private val emailChangeRequestRepository: EmailChangeRequestRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authService: AuthService,
    private val mailer: Mailer,
    private val avatarStorage: AvatarStorage,
    private val verificationCodeGenerator: VerificationCodeGenerator,
) {
    fun me(id: String): User {
        return userRepository.findById(ObjectId(id))
            .orElseThrow { ApiException.NotFound("error.user.not_found") }
    }

    /** Absolute public URL the frontend can use in `<img src>` to fetch the user's avatar,
     *  or null when unset. Host/scheme are derived from the current request, and the
     *  filename is a fresh UUID per upload, so the URL self-busts caches. */
    fun avatarUrl(user: User): String? =
        user.avatarFilename?.let {
            ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/avatars/$it")
                .toUriString()
        }

    @Transactional
    fun uploadAvatar(id: String, file: MultipartFile): User {
        val user = userRepository.findById(ObjectId(id))
            .orElseThrow { ApiException.NotFound("error.user.not_found") }
        val previousFilename = user.avatarFilename
        val newFilename = avatarStorage.store(file)
        val updated = userRepository.save(user.copy(avatarFilename = newFilename))
        // Drop the old file only after the pointer is safely persisted, to avoid orphaning the live avatar.
        previousFilename?.let { avatarStorage.delete(it) }
        return updated
    }

    @Transactional
    fun updateName(id: String, name: String?): User {
        val user = userRepository.findById(ObjectId(id))
            .orElseThrow { ApiException.NotFound("error.user.not_found") }
        if (name == null) return user
        return userRepository.save(user.copy(name = name))
    }

    @Transactional
    fun changePassword(id: String, currentPassword: String, newPassword: String): AuthResponse.Authenticated {
        val user = userRepository.findById(ObjectId(id))
            .orElseThrow { ApiException.NotFound("error.user.not_found") }
        val stored = user.hashedPassword
            ?: throw ApiException.BadRequest("error.user.invalid_current_password")
        if (!passwordEncoder.matches(currentPassword, stored)) {
            throw ApiException.BadRequest("error.user.invalid_current_password")
        }
        val updated = userRepository.save(
            user.copy(hashedPassword = passwordEncoder.encode(newPassword)),
        )
        return authService.reissueAfterPasswordChange(updated)
    }

    @Transactional
    fun changeEmail(id: String, newEmail: String): AuthResponse.VerificationRequired {
        val user = userRepository.findById(ObjectId(id))
            .orElseThrow { ApiException.NotFound("error.user.not_found") }
        if (newEmail == user.email) {
            throw ApiException.BadRequest("error.user.email_same_as_current")
        }
        val taken = userRepository.findByEmail(newEmail)
        if (taken != null && taken.id != user.id) {
            throw ApiException.Conflict("error.auth.email_already_exists")
        }
        emailChangeRequestRepository.deleteByUserId(user.id)
        val plain = verificationCodeGenerator.generate()
        emailChangeRequestRepository.save(
            EmailChangeRequest(
                userId = user.id,
                newEmail = newEmail,
                code = plain.sha256(),
                expiresAt = Instant.now().plus(VERIFICATION_CODE_TTL_MINUTES, ChronoUnit.MINUTES),
            ),
        )
        mailer.sendVerificationCode(newEmail, plain)
        return AuthResponse.VerificationRequired(
            email = newEmail,
            message = "error.user.email_change_code_sent".tr(),
        )
    }

    @Transactional
    fun verifyChangeEmailCode(id: String, code: String): User {
        val user = userRepository.findById(ObjectId(id))
            .orElseThrow { ApiException.NotFound("error.user.not_found") }
        val pending = emailChangeRequestRepository.findByUserId(user.id)
            ?: throw ApiException.BadRequest("error.user.no_pending_email_change")
        if (pending.expiresAt.isBefore(Instant.now())) {
            emailChangeRequestRepository.delete(pending)
            throw ApiException.BadRequest("error.user.email_change_code_expired")
        }
        if (pending.code != code.sha256()) {
            throw ApiException.BadRequest("error.user.email_change_code_invalid")
        }
        val taken = userRepository.findByEmail(pending.newEmail)
        if (taken != null && taken.id != user.id) {
            emailChangeRequestRepository.delete(pending)
            throw ApiException.Conflict("error.auth.email_already_exists")
        }
        val updated = userRepository.save(user.copy(email = pending.newEmail))
        emailChangeRequestRepository.delete(pending)
        return updated
    }
}
