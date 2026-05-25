package com.example.chat.user

import com.example.chat.auth.dto.AuthResponse
import com.example.chat.common.dto.ApiResponse
import com.example.chat.user.dto.ChangePasswordRequest
import com.example.chat.user.dto.ChangePhoneRequest
import com.example.chat.user.dto.ConfirmPhoneChangeRequest
import com.example.chat.user.dto.UpdateRequest
import com.example.chat.user.dto.UserResponse
import com.example.chat.user.mapper.toResponse
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService,
) {
    @GetMapping("/me")
    fun me(
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<UserResponse> {
        val user = userService.me(currentUserId)
        return ApiResponse.ok(user.toResponse(userService.avatarUrl(user)))
    }

    @PatchMapping("/me")
    fun updateProfile(
        @Valid @RequestBody body: UpdateRequest,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<UserResponse> {
        val user = userService.updateProfile(currentUserId, body.name, body.email)
        return ApiResponse.ok(user.toResponse(userService.avatarUrl(user)))
    }

    @PostMapping("/me/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadAvatar(
        @RequestParam("file") file: MultipartFile,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<UserResponse> {
        val user = userService.uploadAvatar(currentUserId, file)
        return ApiResponse.ok(user.toResponse(userService.avatarUrl(user)))
    }

    @PostMapping("/me/change-password")
    fun changePassword(
        @Valid @RequestBody body: ChangePasswordRequest,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<AuthResponse.Authenticated> = ApiResponse.ok(
        userService.changePassword(currentUserId, body.currentPassword, body.newPassword),
    )

    @PostMapping("/me/change-phone/request")
    fun changePhone(
        @Valid @RequestBody body: ChangePhoneRequest,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<AuthResponse.VerificationRequired> = ApiResponse.ok(
        userService.changePhone(currentUserId, body.newPhone),
    )

    @PostMapping("/me/change-phone/verify")
    fun verifyChangePhoneCode(
        @Valid @RequestBody body: ConfirmPhoneChangeRequest,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<UserResponse> {
        val user = userService.verifyChangePhoneCode(currentUserId, body.code)
        return ApiResponse.ok(user.toResponse(userService.avatarUrl(user)))
    }
}
