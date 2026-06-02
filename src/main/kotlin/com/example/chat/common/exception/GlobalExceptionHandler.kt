package com.example.chat.common.exception

import com.example.chat.common.dto.ApiResponse
import com.example.chat.common.extentions.tr
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ApiException::class)
    fun handleApi(ex: ApiException): ResponseEntity<ApiResponse<Nothing>> =
        respond(ex.errorCode, ex.message)

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCreds(ex: BadCredentialsException): ResponseEntity<ApiResponse<Nothing>> =
        respond(ErrorCode.INVALID_CREDENTIALS, ex.message)

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuth(ex: AuthenticationException): ResponseEntity<ApiResponse<Nothing>> =
        respond(ErrorCode.UNAUTHORIZED, ex.message)

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<ApiResponse<Nothing>> =
        respond(ErrorCode.FORBIDDEN, ex.message)

    @ExceptionHandler(DuplicateKeyException::class)
    fun handleDuplicate(ex: DuplicateKeyException): ResponseEntity<ApiResponse<Nothing>> =
        respond(ErrorCode.CONFLICT, ex.message)

    /** Optimistic-lock clash: the document was modified by another request between read and save.
     *  Surfaced as 409 so the client knows to re-fetch and retry rather than a generic 500. */
    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun handleOptimisticLock(ex: OptimisticLockingFailureException): ResponseEntity<ApiResponse<Nothing>> =
        respond(ErrorCode.CONFLICT, "error.concurrent_modification")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleBodyValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val msg = ex.bindingResult.fieldErrors
            .joinToString { "${it.field}: ${it.defaultMessage ?: "invalid"}" }
        return respond(ErrorCode.VALIDATION_ERROR, msg)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleParamValidation(ex: ConstraintViolationException): ResponseEntity<ApiResponse<Nothing>> {
        val msg = ex.constraintViolations.joinToString { "${it.propertyPath}: ${it.message}" }
        return respond(ErrorCode.VALIDATION_ERROR, msg)
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleUploadTooLarge(ex: MaxUploadSizeExceededException): ResponseEntity<ApiResponse<Nothing>> =
        respond(ErrorCode.BAD_REQUEST, "error.user.avatar_too_large")

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArg(ex: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing>> =
        respond(ErrorCode.BAD_REQUEST, ex.message)

    @ExceptionHandler(Exception::class)
    fun handleAny(ex: Exception): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR.code, ex.message?.takeIf { it.isNotBlank() }?.tr() ?: "error.internal".tr()))

    private fun respond(code: ErrorCode, message: String?): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(code.status)
            .body(ApiResponse.fail(code.code, message?.takeIf { it.isNotBlank() }?.tr() ?: code.code))
}
