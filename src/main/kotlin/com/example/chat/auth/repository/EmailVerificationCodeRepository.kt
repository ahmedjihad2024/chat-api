package com.example.chat.auth.repository

import com.example.chat.auth.entities.EmailVerificationCode
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface EmailVerificationCodeRepository : MongoRepository<EmailVerificationCode, ObjectId> {
    fun findByUserId(userId: ObjectId): EmailVerificationCode?
    fun deleteByUserId(userId: ObjectId)
}
