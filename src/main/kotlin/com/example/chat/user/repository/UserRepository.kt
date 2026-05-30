package com.example.chat.user.repository

import com.example.chat.user.entities.User
import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update
import java.time.Instant

interface UserRepository: MongoRepository<User, ObjectId> {
    fun findByPhone(phone: String): User?
    fun findByEmail(email: String): User?
    fun findByName(name: String): User?

    /** Case-insensitive partial match on name OR email, excluding soft-deleted accounts so a
     *  deleted user never surfaces in discovery. [regex] is matched against both fields. */
    @Query("{ 'deleted': { '\$ne': true }, '\$or': [ { 'name': { '\$regex': ?0, '\$options': 'i' } }, { 'email': { '\$regex': ?0, '\$options': 'i' } } ] }")
    fun searchByNameOrEmail(regex: String, pageable: Pageable): Page<User>

    /** Soft-deleted accounts whose grace period has elapsed — the purge job's work list. */
    fun findByDeletedTrueAndDeletedAtBefore(cutoff: Instant): List<User>

    // Atomic ($inc) follow-counter maintenance — race-safe, no read-modify-write.
    @Query("{ '_id': ?0 }")
    @Update("{ '\$inc': { 'followersCount': 1 } }")
    fun incrementFollowersCount(id: ObjectId)

    @Query("{ '_id': ?0 }")
    @Update("{ '\$inc': { 'followersCount': -1 } }")
    fun decrementFollowersCount(id: ObjectId)

    @Query("{ '_id': ?0 }")
    @Update("{ '\$inc': { 'followingCount': 1 } }")
    fun incrementFollowingCount(id: ObjectId)

    @Query("{ '_id': ?0 }")
    @Update("{ '\$inc': { 'followingCount': -1 } }")
    fun decrementFollowingCount(id: ObjectId)
}
