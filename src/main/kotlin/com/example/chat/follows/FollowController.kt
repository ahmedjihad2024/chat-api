package com.example.chat.follows

import com.example.chat.common.dto.ApiResponse
import com.example.chat.follows.dto.ToggleFollowee
import com.example.chat.user.dto.UserResponse
import org.bson.types.ObjectId
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api")
class FollowController(
    private val followService: FollowService,
) {

    /**
     * Toggle following the user [id]: follow if not yet following, unfollow if already following.
     * The actor is always the authenticated user (from the JWT), never taken from the request —
     * so a caller can only follow/unfollow on their own behalf.
     */
    @PostMapping("/follows/toggle-following/{id}")
    fun toggleFollow(
        @PathVariable id: String,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<ToggleFollowee> =
        ApiResponse.ok(followService.toggle(currentUserId, id))

    // --- My own lists ---

    @GetMapping("/follows/followers")
    fun myFollowers(
        @AuthenticationPrincipal currentUserId: String,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): ApiResponse<List<UserResponse>> =
        followService.followers(ObjectId(currentUserId), pageable)

    @GetMapping("/follows/following")
    fun myFollowing(
        @AuthenticationPrincipal currentUserId: String,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): ApiResponse<List<UserResponse>> =
        followService.following(ObjectId(currentUserId), pageable)

    // --- Another user's lists ---

    @GetMapping("/follows/followers/{id}")
    fun userFollowers(
        @PathVariable id: ObjectId,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): ApiResponse<List<UserResponse>> =
        followService.followers(id, pageable)

    @GetMapping("/follows/following/{id}")
    fun userFollowing(
        @PathVariable id: ObjectId,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): ApiResponse<List<UserResponse>> =
        followService.following(id, pageable)
}

