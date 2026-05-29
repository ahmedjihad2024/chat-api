package com.example.chat.config

import com.example.chat.security.ApiResponseAccessDeniedHandler
import com.example.chat.security.ApiResponseAuthenticationEntryPoint
import com.example.chat.security.jwt.JwtAuthFilter
import com.example.chat.security.ratelimit.RateLimitFilter
import com.example.chat.security.ratelimit.RateLimitProperties
import jakarta.servlet.DispatcherType
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

// [Configuration] tells Spring "this class defines beans"
// (the @Bean methods inside become Spring-managed objects).
@Configuration
// [EnableMethodSecurity] turns on annotations like @PreAuthorize("hasRole('ADMIN')") on controller methods.
// Without this, those annotations are ignored.
@EnableMethodSecurity
// [EnableConfigurationProperties(RateLimitProperties::class)]
// Registers RateLimitProperties so Spring binds values
// from application.yml (like app.rate-limit.*) into that class.
@EnableConfigurationProperties(RateLimitProperties::class)
class SecurityConfig(
    @Value($$"${app.cors.allowed-origins:}") private val allowedOrigins: List<String>,
) {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthFilter: JwtAuthFilter,
        rateLimitFilter: RateLimitFilter,
        authEntryPoint: ApiResponseAuthenticationEntryPoint,
        accessDeniedHandler: ApiResponseAccessDeniedHandler,
        corsConfigurationSource: CorsConfigurationSource,
    ): SecurityFilterChain {
        return http
            // (CORS) Cross-Origin Resource Sharing
            .cors { it.configurationSource(corsConfigurationSource) }

            // (CSRF) Cross-Site Request Forgery
            // CSRF only matters for cookie-based auth
            // if you ever switch to cookie-based session auth, you MUST re-enable CSRF.
            .csrf { it.disable() }

            // Never create an HTTP session, never store anything between requests.
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it
                    .requestMatchers(
                        "/api/auth/**",
                        "/avatars/**",
                        "/ws/**",
                        "/actuator/health/**",
                        "/actuator/info",
                        "/actuator/prometheus",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/error/**",
                    ).permitAll()
                    // When an exception happens, Spring internally
                    // forwards the request to an error handler (/error).
                    .dispatcherTypeMatchers(
                        DispatcherType.ERROR,
                        DispatcherType.FORWARD,
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling {
                it
                    // Fires when the user is not authenticated (no token / bad token)
                    .authenticationEntryPoint(authEntryPoint)
                    // Fires when the user IS authenticated but lacks permission (e.g., not an admin)
                    .accessDeniedHandler(accessDeniedHandler)
            }
            // Reads the JWT, validates it, sets the authenticated user
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            // Counts requests per user/IP and blocks if over the limit.
            .addFilterAfter(rateLimitFilter, JwtAuthFilter::class.java)
            .build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOrigins = this@SecurityConfig.allowedOrigins.filter { it.isNotBlank() }
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            // headers the browser sends to you
            allowedHeaders = listOf(
                "Authorization",
                "Content-Type",
                "Accept",
                "Accept-Language",
                "X-Requested-With",   // legacy, some frontends still send it
                "X-Request-Id",       // if you use request tracing
                "Idempotency-Key",    // if you support idempotent POSTs
            )
            //headers the browser lets JS read from you
            exposedHeaders = listOf("Authorization", "X-Request-Id", "X-RateLimit-Remaining")
            allowCredentials = true
            maxAge = 3600
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }

    // Bcrypt hashes passwords with
    // a random salt (so two users with the same password get different hashes)
    // Never store plaintext, never use MD5/SHA-1 for passwords. `Bcrypt`, `Argon2`, or `scrypt` only.
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    // Disable auto-registration
    @Bean
    fun jwtAuthFilterRegistration(filter: JwtAuthFilter): FilterRegistrationBean<JwtAuthFilter> =
        FilterRegistrationBean(filter).apply { isEnabled = false }

    // Disable auto-registration
    @Bean
    fun rateLimitFilterRegistration(filter: RateLimitFilter): FilterRegistrationBean<RateLimitFilter> =
        FilterRegistrationBean(filter).apply { isEnabled = false }
}
