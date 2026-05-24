package com.example.chat.common.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(val code: String, val status: HttpStatus) {
    NOT_FOUND("NOT_FOUND", HttpStatus.NOT_FOUND),
    UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", HttpStatus.FORBIDDEN),
    CONFLICT("CONFLICT", HttpStatus.CONFLICT),
    VALIDATION_ERROR("VALIDATION_ERROR", HttpStatus.BAD_REQUEST),
    BAD_REQUEST("BAD_REQUEST", HttpStatus.BAD_REQUEST),
    RATE_LIMIT_EXCEEDED("RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR),
}
