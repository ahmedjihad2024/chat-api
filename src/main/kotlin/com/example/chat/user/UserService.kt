package com.example.chat.user

import com.example.chat.auth.AuthService
import com.example.chat.auth.dto.AuthResponse
import com.example.chat.auth.repository.RefreshTokenRepository
import com.example.chat.auth.sms.SmsSender
import com.example.chat.auth.sms.VERIFICATION_CODE_TTL_MINUTES
import com.example.chat.auth.sms.VerificationCodeGenerator
import com.example.chat.common.dto.ApiResponse
import com.example.chat.common.extentions.sha256
import com.example.chat.common.exception.ApiException
import com.example.chat.common.extentions.tr
import com.example.chat.follows.FollowService
import com.example.chat.user.dto.UserResponse
import com.example.chat.user.entities.PhoneChangeRequest
import com.example.chat.user.mapper.toResponse
import com.example.chat.user.repository.PhoneChangeRequestRepository
import com.example.chat.user.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.data.domain.Pageable
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
    private val followService: FollowService,
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    fun me(id: String): ApiResponse<UserResponse> {
        val user = userRepository.findById(ObjectId(id))
            .orElseThrow { ApiException.NotFound("error.user.not_found") }
        return ApiResponse.ok(user.toResponse())
    }

    /**
     * Soft-deletes the caller's own account: flags it [User.deleted] and stamps the time, then
     * drops every refresh token so the account is signed out of all devices immediately. The data
     * is retained for [User.DELETED_RETENTION_DAYS] and fully restored if the user logs back in
     * (see [AuthService.login]); otherwise the scheduled purge anonymizes it permanently.
     */
    @Transactional
    fun deleteMe(id: String): ApiResponse<Unit> {
        val userId = ObjectId(id)
        val user = userRepository.findById(userId)
            .orElseThrow { ApiException.NotFound("error.user.not_found") }
        if (!user.deleted) {
            userRepository.updateUserById(
                id = userId,
                deleted = true,
                deletedAt = Instant.now()
            )
        }
        refreshTokenRepository.deleteAllByUserId(userId)
        return ApiResponse.ok(Unit)
    }

    /**
     * Searches users by a case-insensitive partial match on name or email, and tags each result
     * with the [com.example.chat.follows.dto.FollowRelation] to [currentUserId]. The query is
     * regex-escaped so user input is treated as a literal substring.
     */
    fun search(currentUserId: String, query: String, pageable: Pageable): ApiResponse<List<UserResponse>> {
        val me = ObjectId(currentUserId)
        val escaped = Regex.escape(query.trim())
        val page = userRepository.searchByNameOrEmail(escaped, pageable)
        val relations = followService.relationsFrom(me, page.content.map { it.id })
        return ApiResponse.paged(page.map { it.toResponse(relations[it.id]) })
    }

    @Transactional
    fun uploadAvatar(id: String, file: MultipartFile): ApiResponse<UserResponse> {
        val user = userRepository.findById(ObjectId(id))
            .orElseThrow { ApiException.NotFound("error.user.not_found") }
        val previousFilename = user.avatarFilename
        val newFilename = avatarStorage.store(file)
        // Targeted update: set only avatarFilename, so a concurrent follow ($inc) or other change
        // is never clobbered by a stale full-document write.
        val updated = userRepository.updateUserById(id = user.id, avatarFilename = newFilename)
            ?: throw ApiException.NotFound("error.user.not_found")
        // Drop the old file only after the pointer is safely persisted, to avoid orphaning the live avatar.
        previousFilename?.let { avatarStorage.delete(it) }
        return ApiResponse.ok(updated.toResponse())
    }

    /** Updates name/email (each applied only when non-null). Targeted update so concurrent
     *  changes aren't clobbered; the sparse-unique index guards against duplicate emails. */
    @Transactional
    fun updateProfile(id: String, name: String?, email: String?): ApiResponse<UserResponse> {
        val user = userRepository.findById(ObjectId(id))
            .orElseThrow { ApiException.NotFound("error.user.not_found") }
        if (email != null && email != user.email) {
            val taken = userRepository.findByEmail(email)
            if (taken != null && taken.id != user.id) {
                throw ApiException.Conflict("error.auth.email_already_exists")
            }
        }
        // Nothing actually changed -> skip the write.
        if ((name == null || name == user.name) && (email == null || email == user.email)) {
            return ApiResponse.ok(user.toResponse())
        }
        val updated = userRepository.updateUserById(id = user.id, name = name, email = email)
            ?: throw ApiException.NotFound("error.user.not_found")
        return ApiResponse.ok(updated.toResponse())
    }

    @Transactional
    fun changePassword(
        id: String,
        currentPassword: String,
        newPassword: String
    ): ApiResponse<AuthResponse.Authenticated> {
        val user = userRepository.findById(ObjectId(id))
            .orElseThrow { ApiException.NotFound("error.user.not_found") }
        val stored = user.hashedPassword
            ?: throw ApiException.BadRequest("error.user.invalid_current_password")
        if (!passwordEncoder.matches(currentPassword, stored)) {
            throw ApiException.BadRequest("error.user.invalid_current_password")
        }
        // Targeted update: only hashedPassword is written, never the whole document.
        val updated = userRepository.updateUserById(
            id = user.id,
            hashedPassword = passwordEncoder.encode(newPassword),
        ) ?: throw ApiException.NotFound("error.user.not_found")
        return ApiResponse.ok(authService.reissueAfterPasswordChange(updated))
    }

    /** Phone is the verified identifier, so changing it requires SMS confirmation of the new number. */
    @Transactional
    fun changePhone(id: String, newPhone: String): ApiResponse<AuthResponse.VerificationRequired> {
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
        return ApiResponse.ok(
            AuthResponse.VerificationRequired(
                phone = newPhone,
                message = "error.user.phone_change_code_sent".tr(),
            ),
        )
    }

    @Transactional
    fun verifyChangePhoneCode(id: String, code: String): ApiResponse<UserResponse> {
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

        // Targeted update: only phone is written, never the whole document.
        val updated = userRepository.updateUserById(id = user.id, phone = pending.newPhone)
            ?: throw ApiException.NotFound("error.user.not_found")
        phoneChangeRequestRepository.delete(pending)

        return ApiResponse.ok(updated.toResponse())
    }
}
