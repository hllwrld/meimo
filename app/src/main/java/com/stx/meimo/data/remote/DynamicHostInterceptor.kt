package com.stx.meimo.data.remote

import com.stx.meimo.util.ServerConfig
import okhttp3.Interceptor
import okhttp3.Response

class DynamicHostInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val newUrl = request.url.newBuilder()
            .host(ServerConfig.currentDomain)
            .build()
        return chain.proceed(request.newBuilder().url(newUrl).build())
    }
}
