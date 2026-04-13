package com.stx.meimo.data.remote

import com.stx.meimo.util.TokenStorage
import com.stx.meimo.util.generateIdempotencyKey
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenStorage: TokenStorage) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()
            .header("Content-Type", "application/json")
            .header("Lang", "ZH")
            .header("Idempotency-Key", generateIdempotencyKey())

        // Send stored token as Authorization header.
        // The login API returns a token (even "FAKE" ones) — the web app sends it as Authorization.
        // The rptoken cookie (set by server via Set-Cookie) is handled by OkHttp's CookieJar.
        tokenStorage.getToken()?.let { token ->
            builder.header("Authorization", token)
        }

        return chain.proceed(builder.build())
    }
}
