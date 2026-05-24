package com.example.chat.security

import com.example.chat.common.dto.ApiResponse
import com.example.chat.common.exception.ErrorCode
import com.example.chat.common.extentions.tr
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ApiResponseAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        val body = ApiResponse.fail(ErrorCode.UNAUTHORIZED.code, "error.unauthorized.default".tr())
        objectMapper.writeValue(response.outputStream, body)
    }
}
