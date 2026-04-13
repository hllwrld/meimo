package com.stx.meimo.data.repository

import com.stx.meimo.data.model.LoginRequest
import com.stx.meimo.data.model.UserDto
import com.stx.meimo.data.remote.MeimoApi
import com.stx.meimo.util.ApiException
import com.stx.meimo.util.TokenStorage

class AuthRepository(
    private val api: MeimoApi,
    private val tokenStorage: TokenStorage
) {

    suspend fun login(username: String, password: String): Result<UserDto> = runCatching {
        val response = api.login(LoginRequest(username = username, password = password))
        if (response.code != 200 || response.data == null) {
            throw ApiException(response.code, response.message ?: "Login failed")
        }
        // Save token from login response — even "FAKE" tokens are used as Authorization header
        val token = response.data.token
        if (!token.isNullOrBlank()) {
            tokenStorage.saveToken(token)
        }
        tokenStorage.setLoggedIn(true)
        response.data
    }

    suspend fun getUserInfo(): Result<UserDto> = runCatching {
        val response = api.getUserInfo()
        if (response.code != 200 || response.data == null) {
            throw ApiException(response.code, response.message ?: "Failed to get user info")
        }
        response.data
    }

    fun logout() {
        tokenStorage.clearToken()
        tokenStorage.setLoggedIn(false)
        // Clear persistent cookie jar
        try {
            com.stx.meimo.di.AppModule.cookieJar.clear()
        } catch (_: Exception) { }
    }

    val isLoggedIn: Boolean get() = tokenStorage.isLoggedIn
}
