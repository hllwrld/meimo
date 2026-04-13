package com.stx.meimo.util

class ApiException(
    val code: Int,
    override val message: String
) : Exception("API error $code: $message")
