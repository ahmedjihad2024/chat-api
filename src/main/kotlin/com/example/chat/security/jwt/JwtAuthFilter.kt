package com.example.chat.security.jwt

import com.example.chat.auth.repository.RevokedAccessTokenRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
    private val revokedAccessTokenRepository: RevokedAccessTokenRepository,
): OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            if (jwtService.validateAccessToken(authHeader)) {
                val jti = jwtService.getJti(authHeader)
                if (!revokedAccessTokenRepository.existsByJti(jti)) {
                    val userId = jwtService.getUserIdFromToken(authHeader)
                    val authorities = jwtService.getRolesFromToken(authHeader)
                        .map { SimpleGrantedAuthority(it.authority()) }
                    val auth = UsernamePasswordAuthenticationToken(userId, null, authorities)
                    SecurityContextHolder.getContext().authentication = auth
                }
            }
        }
        filterChain.doFilter(request, response)
    }

}
