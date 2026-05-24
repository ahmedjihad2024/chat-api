package com.example.chat.security

import com.example.chat.common.dto.ApiResponse
import com.example.chat.common.exception.ErrorCode
import com.example.chat.common.extentions.tr
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ApiResponseAccessDeniedHandler(
    private val objectMapper: ObjectMapper,
) : AccessDeniedHandler {

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        val body = ApiResponse.fail(ErrorCode.FORBIDDEN.code, "error.access_denied.default".tr())
        objectMapper.writeValue(response.outputStream, body)
    }
}
