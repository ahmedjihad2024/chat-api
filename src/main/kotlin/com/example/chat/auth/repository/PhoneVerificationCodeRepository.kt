package com.example.chat.auth.repository

import com.example.chat.auth.entities.PhoneVerificationCode
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface PhoneVerificationCodeRepository : MongoRepository<PhoneVerificationCode, ObjectId> {
    fun findByUserId(userId: ObjectId): PhoneVerificationCode?
    fun deleteByUserId(userId: ObjectId)
}
