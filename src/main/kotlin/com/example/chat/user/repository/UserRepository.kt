package com.example.chat.user.repository

import com.example.chat.user.entities.User
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository: MongoRepository<User, ObjectId> {
    fun findByPhone(phone: String): User?
    fun findByEmail(email: String): User?
    fun findByName(name: String): User?
}