package com.example.chat.user.repository

import com.example.chat.user.entities.PhoneChangeRequest
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface PhoneChangeRequestRepository : MongoRepository<PhoneChangeRequest, ObjectId> {
    fun findByUserId(userId: ObjectId): PhoneChangeRequest?
    fun deleteByUserId(userId: ObjectId)
}
