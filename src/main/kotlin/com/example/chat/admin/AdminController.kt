package com.example.chat.admin

import com.example.chat.admin.dto.UpdateUserRolesRequest
import com.example.chat.common.dto.ApiResponse
import com.example.chat.common.dto.PageResponse
import com.example.chat.user.dto.UserResponse
import com.example.chat.user.mapper.toResponse
import jakarta.validation.Valid
import org.bson.types.ObjectId
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController(
    private val adminService: AdminService,
) {

    @GetMapping("/users")
    fun listUsers(
        @PageableDefault(size = 20, sort = ["name"], direction = Sort.Direction.ASC)
        pageable: Pageable,
    ): ApiResponse<PageResponse<UserResponse>> =
        ApiResponse.ok(PageResponse.from(adminService.listUsers(pageable)) { it.toResponse() })

    @PatchMapping("/users/{id}/roles")
    fun updateRoles(
        @PathVariable id: ObjectId,
        @Valid @RequestBody body: UpdateUserRolesRequest,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<UserResponse> =
        ApiResponse.ok(adminService.updateRoles(id, currentUserId, body.roles).toResponse())
}
