package com.example.chat.follows

import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface FollowsRepository: MongoRepository<Follows, ObjectId> {

    // Unfollow: delete the one edge. Returns how many were removed (0 = wasn't following).
    fun deleteByFollowerIdAndFolloweeId(followerId: ObjectId, followeeId: ObjectId): Long

    // "Am I following X?" — also lets the service reject a duplicate follow before inserting.
    fun existsByFollowerIdAndFolloweeId(followerId: ObjectId, followeeId: ObjectId): Boolean

    // "Who I follow" (following list), paged.   → uses the (followerId, followeeId) index
    fun findByFollowerId(followerId: ObjectId, pageable: Pageable): Page<Follows>

    // "Who follows me" (followers list), paged.  → uses the (followeeId, followerId) index
    fun findByFolloweeId(followeeId: ObjectId, pageable: Pageable): Page<Follows>

    // Counts for the profile screen.
    fun countByFollowerId(followerId: ObjectId): Long   // how many I follow
    fun countByFolloweeId(followeeId: ObjectId): Long   // how many follow me

    // Batch relationship lookups against a set of users (for computing FollowRelation in a list).
    fun findByFollowerIdAndFolloweeIdIn(followerId: ObjectId, followeeIds: Collection<ObjectId>): List<Follows>
    fun findByFolloweeIdAndFollowerIdIn(followeeId: ObjectId, followerIds: Collection<ObjectId>): List<Follows>
}