package com.example.chat.common.dto

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.data.domain.Page

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val status: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
    val meta: Any? = null,
) {
    companion object {
        fun <T> ok(data: T, meta: Any? = null): ApiResponse<T> =
            ApiResponse(status = true, data = data, meta = meta)

        /**
         * A plain page: `{ data: [...], meta: { ...pagination } }`.
         * Map entities to their response DTOs before calling this (e.g. in the service layer).
         */
        fun <T : Any> paged(page: Page<T>): ApiResponse<List<T>> =
            ok(data = page.content, meta = PageMeta.from(page))

        /**
         * A page that carries its own extra [meta] alongside the list, nested under `data`:
         * `{ data: { meta: { ...extra }, data: [...] }, meta: { ...pagination } }`.
         */
        fun <T : Any, M : Any> paged(page: Page<T>, meta: M): ApiResponse<MetaData<M, List<T>>> =
            ok(data = MetaData(meta = meta, data = page.content), meta = PageMeta.from(page))

        fun fail(code: String, message: String): ApiResponse<Nothing> =
            ApiResponse(status = false, error = ApiError(code, message))
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiError(
    val code: String,
    val message: String,
)

/** A `{ meta, data }` envelope used to attach endpoint-specific [meta] to a payload. */
data class MetaData<M, D>(
    val meta: M,
    val data: D,
)

/** Pagination details for a [Page], one-indexed to match the API's page parameter. */
data class PageMeta(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
) {
    companion object {
        fun from(page: Page<*>): PageMeta = PageMeta(
            // Spring's Page.number is always zero-based internally, but the API
            // is configured with one-indexed-parameters=true. Shift by +1 so the
            // response matches the page number the client sent.
            page = page.number + 1,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious(),
        )
    }
}
