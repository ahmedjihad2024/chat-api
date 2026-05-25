package com.example.chat.follows

import com.example.chat.common.dto.ApiResponse
import com.example.chat.common.exception.ApiException
import com.example.chat.follows.dto.FollowRelation
import com.example.chat.follows.dto.ToggleFollowee
import com.example.chat.user.dto.UserResponse
import com.example.chat.user.mapper.toResponse
import com.example.chat.user.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FollowService(
    private val followsRepository: FollowsRepository,
    private val userRepository: UserRepository,
) {

    /**
     * One idempotent toggle: follows the target if not already following, otherwise unfollows.
     * Returns the resulting state (`following = true` means you now follow them).
     *
     * The `follows` edge is the source of truth and is written first; the denormalized User
     * counters are then adjusted with atomic `$inc` (race-safe — concurrent follows can't lose
     * an update). Counters are derived data, so if they ever drift they can be rebuilt by
     * counting the `follows` collection.
     */
    @Transactional
    fun toggle(followerId: String, followeeId: String): ToggleFollowee {
        val followerId = ObjectId(followerId)
        val followeeId = ObjectId(followeeId)

        if (followerId == followeeId) {
            throw ApiException.BadRequest("error.follow.cannot_follow_self")
        }
        if (!userRepository.existsById(followeeId)) {
            throw ApiException.NotFound("error.user.not_found")
        }

        return if (followsRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            unfollow(followerId, followeeId)
            ToggleFollowee(following = false)
        } else {
            follow(followerId, followeeId)
            ToggleFollowee(following = true)
        }
    }

    private fun follow(followerId: ObjectId, followeeId: ObjectId) {
        // Edge first (authoritative); the unique compound index blocks accidental duplicates.
        followsRepository.save(Follows(followerId = followerId, followeeId = followeeId))
        userRepository.incrementFollowersCount(followeeId)
        userRepository.incrementFollowingCount(followerId)
    }

    private fun unfollow(followerId: ObjectId, followeeId: ObjectId) {
        val removed = followsRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId)
        if (removed == 0L) return // nothing was there — don't decrement below the real total
        userRepository.decrementFollowersCount(followeeId)
        userRepository.decrementFollowingCount(followerId)
    }

    /** People who follow [userId] (the followers list), one page at a time. */
    @Transactional(readOnly = true)
    fun followers(userId: ObjectId, pageable: Pageable): ApiResponse<List<UserResponse>> {
        userRepository.findById(userId)
            .orElseThrow { ApiException.NotFound("error.user.not_found") }
        val page = followsRepository.findByFolloweeId(userId, pageable)
        return ApiResponse.paged(page.mapInOrder { it.followerId })
    }

    /** People [userId] follows (the following list), one page at a time. */
    @Transactional(readOnly = true)
    fun following(userId: ObjectId, pageable: Pageable): ApiResponse<List<UserResponse>> {
        userRepository.findById(userId)
            .orElseThrow { ApiException.NotFound("error.user.not_found") }
        val page = followsRepository.findByFollowerId(userId, pageable)
        return ApiResponse.paged(page.mapInOrder { it.followeeId })
    }

    /**
     * The [FollowRelation] from [currentUserId] to each of [targetIds], computed in two batch
     * queries. Ids with no edge in either direction map to [FollowRelation.NONE].
     */
    @Transactional(readOnly = true)
    fun relationsFrom(currentUserId: ObjectId, targetIds: List<ObjectId>): Map<ObjectId, FollowRelation> {
        if (targetIds.isEmpty()) return emptyMap()
        val iFollow = followsRepository.findByFollowerIdAndFolloweeIdIn(currentUserId, targetIds)
            .mapTo(HashSet()) { it.followeeId }
        val followMe = followsRepository.findByFolloweeIdAndFollowerIdIn(currentUserId, targetIds)
            .mapTo(HashSet()) { it.followerId }
        return targetIds.associateWith { id ->
            val outgoing = id in iFollow
            val incoming = id in followMe
            when {
                outgoing && incoming -> FollowRelation.FRIEND
                outgoing -> FollowRelation.FOLLOWING
                incoming -> FollowRelation.FOLLOWER
                else -> FollowRelation.NONE
            }
        }
    }

    /** Maps a page of follow edges to the referenced users, batch-loaded and kept in page order. */
    private fun Page<Follows>.mapInOrder(idSelector: (Follows) -> ObjectId): Page<UserResponse> =
        PageImpl(usersInOrder(content.map(idSelector)), pageable, totalElements)

    /** Loads the given users in one query and returns them as responses, preserving [ids] order. */
    private fun usersInOrder(ids: List<ObjectId>): List<UserResponse> {
        if (ids.isEmpty()) return emptyList()
        val byId = userRepository.findAllById(ids).associateBy { it.id }
        return ids.mapNotNull { id -> byId[id]?.toResponse() }
    }
}
