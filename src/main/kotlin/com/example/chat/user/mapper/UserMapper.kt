package com.example.chat.user.mapper

import com.example.chat.common.extentions.toAvatarUrl
import com.example.chat.follows.dto.FollowRelation
import com.example.chat.user.dto.UserResponse
import com.example.chat.user.entities.User

fun User.toResponse(relation: FollowRelation? = null): UserResponse = UserResponse(
    id = id.toHexString(),
    name = name,
    phone = phone,
    email = email,
    roles = roles.map { it.name }.toSet(),
    phoneVerified = phoneVerified,
    avatar = avatarFilename.toAvatarUrl(),
    followersCount = followersCount,
    followingCount = followingCount,
    relation = relation,
)

/**
 * How a (possibly deleted) user appears to a chat partner. A live user maps normally; a
 * soft-deleted one is masked to the placeholder name with no avatar, so existing threads keep
 * working without leaking the deleted account's identity. Once the purge runs the document is
 * already anonymized, so this returns the same masked view.
 */
fun User.toConversationParticipant(): UserResponse =
    if (deleted) toResponse().copy(name = User.DELETED_DISPLAY_NAME, avatar = null)
    else toResponse()
