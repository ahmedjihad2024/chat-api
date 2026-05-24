package com.example.chat.auth.repository

import com.example.chat.auth.entities.RevokedAccessToken
import org.springframework.data.mongodb.repository.MongoRepository

interface RevokedAccessTokenRepository : MongoRepository<RevokedAccessToken, String> {
    fun existsByJti(jti: String): Boolean
}
