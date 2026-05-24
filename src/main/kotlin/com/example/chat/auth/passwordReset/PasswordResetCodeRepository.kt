package com.example.chat.auth.passwordReset

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface PasswordResetCodeRepository : MongoRepository<PasswordResetCode, ObjectId> {
    fun findByUserId(userId: ObjectId): PasswordResetCode?
    fun deleteByUserId(userId: ObjectId)
}