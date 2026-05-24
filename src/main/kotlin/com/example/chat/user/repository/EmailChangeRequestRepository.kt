package com.example.chat.user.repository

import com.example.chat.user.entities.EmailChangeRequest
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface EmailChangeRequestRepository : MongoRepository<EmailChangeRequest, ObjectId> {
    fun findByUserId(userId: ObjectId): EmailChangeRequest?
    fun deleteByUserId(userId: ObjectId)
}
