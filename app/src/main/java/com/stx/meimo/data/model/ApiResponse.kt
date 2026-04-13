package com.stx.meimo.data.model

data class ApiResponse<T>(
    val code: Int,
    val data: T?,
    val message: String?
)

data class PagedData<T>(
    val total: Int,
    val content: List<T>
)
