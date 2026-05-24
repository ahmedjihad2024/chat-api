package com.example.chat.common.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val status: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
    val meta: Map<String, Any>? = null,
) {
    companion object {
        fun <T> ok(data: T, meta: Map<String, Any>? = null): ApiResponse<T> =
            ApiResponse(status = true, data = data, meta = meta)

        fun fail(code: String, message: String): ApiResponse<Nothing> =
            ApiResponse(status = false, error = ApiError(code, message))
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiError(
    val code: String,
    val message: String,
)
