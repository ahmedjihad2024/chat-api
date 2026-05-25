package com.example.chat.user

import com.example.chat.auth.AuthService
import com.example.chat.auth.dto.AuthResponse
import com.example.chat.auth.sms.SmsSender
import com.example.chat.auth.sms.VERIFICATION_CODE_TTL_MINUTES
import com.example.chat.auth.sms.VerificationCodeGenerator
import com.example.chat.common.extentions.sha256
import com.example.chat.common.exception.ApiException
import com.example.chat.common.extentions.tr
import com.example.chat.user.dto.UserResponse
import com.example.chat.user.entities.PhoneChangeRequest
import com.example.chat.user.mapper.toResponse
import com.example.chat.user.repository.PhoneChangeRequestRepository
import com.example.chat.user.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class UserService(
    private val userRepository: UserRepository,
    private val phoneChangeRequestRepository: PhoneChangeRequestRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authService: AuthService,
    private val smsSender: SmsSender,
    private val avatarStorage: AvatarStorage,
    private val verificationCodeGenerator: VerificationCodeGenerator,
) {
    fun me(id: String): UserResponse {
        val user = userRepository.findById(ObjectId(id))
            .orElseThrow { ApiException.NotFound("error.user.not_found") }
        return user.toResponse()
    }

    @Transactional
    fun uploadAvatar(id: String, file: MultipartFile): UserResponse {
        val user = userRepository.findById(ObjectId(id))
            .orElseThrow { ApiException.NotFound("error.user.not_found") }
        val previousFilename = user.avatarFilename
        val newFilename = avatarStorage.store(file)
        val updated = userRepository.save(user.copy(avatarFilename = newFilename))
        // Drop the old file only after the pointer is safely persisted, to avoid orphaning the live avatar.
        previousFilename?.let { avatarStorage.delete(it) }
        return updated.toResponse()
    }

    /** Updates the mutable profile fields. Each argument is applied only when non-null;
     *  a null leaves that field unchanged. Email is optional/unverified and set directly,
     *  with a sparse-unique check so two accounts can't share one. */
    @Transactional
    fun updateProfile(id: String, name: String?, email: String?): UserResponse {
        val user = userRepository.findById(ObjectId(id))
            .orElseThrow { ApiException.NotFound("error.user.not_found") }
        if (email != null && email != user.email) {
            val taken = userRepository.findByEmail(email)
            if (taken != null && taken.id != user.id) {
                throw ApiException.Conflict("error.auth.email_already_exists")
            }
        }
        val updated = user.copy(
            name = name ?: user.name,
            email = email ?: user.email,
        )
        if (updated == user) return user.toResponse()
        return userRepository.save(updated).toResponse()
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

    /** Phone is the verified identifier, so changing it requires SMS confirmation of the new number. */
    @Transactional
    fun changePhone(id: String, newPhone: String): AuthResponse.VerificationRequired {
        val user = userRepository.findById(ObjectId(id))
            .orElseThrow { ApiException.NotFound("error.user.not_found") }
        if (newPhone == user.phone) {
            throw ApiException.BadRequest("error.user.phone_same_as_current")
        }
        val taken = userRepository.findByPhone(newPhone)
        if (taken != null && taken.id != user.id) {
            throw ApiException.Conflict("error.auth.phone_already_exists")
        }
        phoneChangeRequestRepository.deleteByUserId(user.id)
        val plain = verificationCodeGenerator.generate()
        phoneChangeRequestRepository.save(
            PhoneChangeRequest(
                userId = user.id,
                newPhone = newPhone,
                code = plain.sha256(),
                expiresAt = Instant.now().plus(VERIFICATION_CODE_TTL_MINUTES, ChronoUnit.MINUTES),
            ),
        )
        smsSender.sendVerificationCode(newPhone, plain)
        return AuthResponse.VerificationRequired(
            phone = newPhone,
            message = "error.user.phone_change_code_sent".tr(),
        )
    }

    @Transactional
    fun verifyChangePhoneCode(id: String, code: String): UserResponse {
        val user = userRepository.findById(ObjectId(id))
            .orElseThrow { ApiException.NotFound("error.user.not_found") }
        val pending = phoneChangeRequestRepository.findByUserId(user.id)
            ?: throw ApiException.BadRequest("error.user.no_pending_phone_change")
        if (pending.expiresAt.isBefore(Instant.now())) {
            phoneChangeRequestRepository.delete(pending)
            throw ApiException.BadRequest("error.user.phone_change_code_expired")
        }
        if (pending.code != code.sha256()) {
            throw ApiException.BadRequest("error.user.phone_change_code_invalid")
        }
        val taken = userRepository.findByPhone(pending.newPhone)
        if (taken != null && taken.id != user.id) {
            phoneChangeRequestRepository.delete(pending)
            throw ApiException.Conflict("error.auth.phone_already_exists")
        }
        val updated = userRepository.save(user.copy(phone = pending.newPhone))
        phoneChangeRequestRepository.delete(pending)
        return updated.toResponse()
    }
}
