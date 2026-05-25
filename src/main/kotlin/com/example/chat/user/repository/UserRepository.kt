package com.example.chat.user.repository

import com.example.chat.user.entities.User
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update

interface UserRepository: MongoRepository<User, ObjectId> {
    fun findByPhone(phone: String): User?
    fun findByEmail(email: String): User?
    fun findByName(name: String): User?

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
