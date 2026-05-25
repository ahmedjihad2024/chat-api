package com.example.chat.follows.dto

import com.example.chat.user.dto.UserResponse

sealed class FollowsResponse {
    data class Follows(
        val count: Int,
        val data: List<UserResponse>,
    ) : FollowsResponse()

    data class ToggleFollowee(
        val following: Boolean,
    ) : FollowsResponse()
}
