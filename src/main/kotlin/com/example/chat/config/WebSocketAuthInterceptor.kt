package com.example.chat.config

import com.example.chat.auth.repository.RevokedAccessTokenRepository
import com.example.chat.security.jwt.JwtService
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.stereotype.Component
import java.security.Principal

/**
 * Authenticates the STOMP `CONNECT` frame the same way [com.example.chat.security.jwt.JwtAuthFilter]
 * authenticates REST requests: it reads the `Authorization: Bearer <jwt>` native header, validates
 * the access token (and checks it isn't revoked), and sets the authenticated user as the session
 * principal. The principal's name is the user id, which is what `convertAndSendToUser` routes on.
 *
 * A CONNECT without a valid token is rejected, so every socket session is authenticated before it
 * can send to application destinations or subscribe to user destinations.
 */
@Component
class WebSocketAuthInterceptor(
    private val jwtService: JwtService,
    private val revokedAccessTokenRepository: RevokedAccessTokenRepository,
) : ChannelInterceptor {
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
        if (accessor != null && StompCommand.CONNECT == accessor.command) {
            val userId = authenticate(accessor.getFirstNativeHeader("Authorization"))
                ?: throw IllegalArgumentException("error.unauthorized.default")
            accessor.user = StompPrincipal(userId)
        }
        return message
    }

    /** Returns the authenticated user id for a valid, non-revoked access token, or null. */
    private fun authenticate(authHeader: String?): String? {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null
        if (!jwtService.validateAccessToken(authHeader)) return null
        val jti = jwtService.getJti(authHeader)
        if (revokedAccessTokenRepository.existsByJti(jti)) return null
        return jwtService.getUserIdFromToken(authHeader)
    }
}

/** Minimal [Principal] whose name is the authenticated user id (used for user-destination routing). */
data class StompPrincipal(private val userId: String) : Principal {
    override fun getName(): String = userId
}
