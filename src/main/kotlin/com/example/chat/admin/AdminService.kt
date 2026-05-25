package com.example.chat.admin

import com.example.chat.common.exception.ApiException
import com.example.chat.user.dto.UserResponse
import com.example.chat.user.enums.Role
import com.example.chat.user.entities.User
import com.example.chat.user.mapper.toResponse
import com.example.chat.user.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class AdminService(
    private val userRepository: UserRepository,
) {

    fun listUsers(pageable: Pageable): Page<UserResponse> =
        userRepository.findAll(pageable).map { it.toResponse() }

    fun updateRoles(targetUserId: ObjectId, currentUserId: String, roleNames: Set<String>): User {
        if (targetUserId.toHexString() == currentUserId) {
            throw ApiException.BadRequest("error.admin.cannot_modify_self")
        }
        val roles = roleNames.map { name ->
            runCatching { Role.valueOf(name) }.getOrElse {
                throw ApiException.BadRequest("error.admin.invalid_role")
            }
        }.toSet()
        val user = userRepository.findById(targetUserId).orElseThrow { ApiException.NotFound("User") }
        return userRepository.save(user.copy(roles = roles))
    }
}
