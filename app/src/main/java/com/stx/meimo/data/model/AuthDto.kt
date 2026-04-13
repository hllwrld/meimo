package com.stx.meimo.data.model

data class LoginRequest(
    val username: String,
    val password: String,
    val code: String = "",
    val inviteCode: String = "",
    val deviceId: String = ""
)
