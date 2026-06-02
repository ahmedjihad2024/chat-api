package com.example.chat.security.jwt

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
    private val tokenBlacklist: TokenBlacklist,
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
                if (!tokenBlacklist.isRevoked(jti)) {
                    val userId = jwtService.getUserIdFromToken(authHeader)
                    val authorities = jwtService.getRolesFromToken(authHeader)
                        .map { SimpleGrantedAuthority(it.authority()) }
                    val auth = UsernamePasswordAuthenticationToken(userId, null, authorities)
                    // Stores the authenticated user in a ThreadLocal scoped to this request's thread,
                    // so downstream filters (authorization), @PreAuthorize, and @AuthenticationPrincipal can read it.
                    // 3-arg ctor marks isAuthenticated=true; Spring clears the context after the request ends.
                    val context = SecurityContextHolder.createEmptyContext()
                    context.authentication = auth
                    SecurityContextHolder.setContext(context)
                }
            }
        }
        filterChain.doFilter(request, response)
    }

}
