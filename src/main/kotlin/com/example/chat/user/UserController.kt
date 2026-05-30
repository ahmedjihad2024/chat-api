package com.example.chat.user

import com.example.chat.auth.dto.AuthResponse
import com.example.chat.common.dto.ApiResponse
import com.example.chat.user.dto.ChangePasswordRequest
import com.example.chat.user.dto.ChangePhoneRequest
import com.example.chat.user.dto.ConfirmPhoneChangeRequest
import com.example.chat.user.dto.UpdateRequest
import com.example.chat.user.dto.UserResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
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
    ): ApiResponse<UserResponse> =
        userService.me(currentUserId)

    @GetMapping("/search")
    fun search(
        @RequestParam q: String,
        @AuthenticationPrincipal currentUserId: String,
        @PageableDefault(size = 20, sort = ["name"], direction = Sort.Direction.ASC) pageable: Pageable,
    ): ApiResponse<List<UserResponse>> =
        userService.search(currentUserId, q, pageable)

    @DeleteMapping("/me")
    fun deleteMe(
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<Unit> =
        userService.deleteMe(currentUserId)

    @PatchMapping("/me")
    fun updateProfile(
        @Valid @RequestBody body: UpdateRequest,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<UserResponse> =
        userService.updateProfile(currentUserId, body.name, body.email)

    @PostMapping("/me/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadAvatar(
        @RequestParam("file") file: MultipartFile,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<UserResponse> =
        userService.uploadAvatar(currentUserId, file)

    @PostMapping("/me/change-password")
    fun changePassword(
        @Valid @RequestBody body: ChangePasswordRequest,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<AuthResponse.Authenticated> =
        userService.changePassword(currentUserId, body.currentPassword, body.newPassword)

    @PostMapping("/me/change-phone/request")
    fun changePhone(
        @Valid @RequestBody body: ChangePhoneRequest,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<AuthResponse.VerificationRequired> =
        userService.changePhone(currentUserId, body.newPhone)

    @PostMapping("/me/change-phone/verify")
    fun verifyChangePhoneCode(
        @Valid @RequestBody body: ConfirmPhoneChangeRequest,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<UserResponse> =
        userService.verifyChangePhoneCode(currentUserId, body.code)
}
