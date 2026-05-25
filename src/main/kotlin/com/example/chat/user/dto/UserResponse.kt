package com.example.chat.user.dto

import com.example.chat.follows.dto.FollowRelation
import com.fasterxml.jackson.annotation.JsonInclude

data class UserResponse(
    val id: String,
    val name: String,
    val phone: String,
    val email: String?,
    val roles: Set<String>,
    val phoneVerified: Boolean,
    val avatar: String? = null,
    val followersCount: Int,
    val followingCount: Int,
    // Only set for "other user" views (e.g. search); omitted from the JSON otherwise.
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val relation: FollowRelation? = null,
)
