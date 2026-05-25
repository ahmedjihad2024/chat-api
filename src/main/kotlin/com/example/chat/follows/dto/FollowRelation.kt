package com.example.chat.follows.dto

/** The follow relationship between the current user and another user, from the current user's view. */
enum class FollowRelation {
    /** The current user follows them, but not the other way around. */
    FOLLOWING,

    /** They follow the current user, but not the other way around. */
    FOLLOWER,

    /** They follow each other. */
    FRIEND,

    /** No follow edge in either direction. */
    NONE,
}
