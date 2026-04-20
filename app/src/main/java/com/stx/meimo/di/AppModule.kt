package com.stx.meimo.di

import android.content.Context
import com.stx.meimo.data.remote.AuthInterceptor
import com.stx.meimo.data.remote.DynamicHostInterceptor
import com.stx.meimo.data.remote.MeimoApi
import com.stx.meimo.data.remote.SseClient
import com.stx.meimo.data.repository.AuthRepository
import com.stx.meimo.data.repository.ChatRepository
import com.stx.meimo.data.repository.ModelRepository
import com.stx.meimo.data.repository.RewardRepository
import com.stx.meimo.data.repository.RoleRepository
import com.stx.meimo.util.AppPreferences
import com.stx.meimo.util.PersistentCookieJar
import com.stx.meimo.util.ServerConfig
import com.stx.meimo.util.TokenStorage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AppModule {

    lateinit var tokenStorage: TokenStorage
        private set

    lateinit var cookieJar: PersistentCookieJar
        private set

    lateinit var appPreferences: AppPreferences
        private set

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(DynamicHostInterceptor())
            .addInterceptor(AuthInterceptor(tokenStorage))
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ServerConfig.apiBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: MeimoApi by lazy { retrofit.create(MeimoApi::class.java) }

    val sseClient: SseClient by lazy { SseClient(okHttpClient) }

    val authRepository: AuthRepository by lazy { AuthRepository(api, tokenStorage) }
    val roleRepository: RoleRepository by lazy { RoleRepository(api) }
    val chatRepository: ChatRepository by lazy { ChatRepository(api) }
    val modelRepository: ModelRepository by lazy { ModelRepository(api) }
    val rewardRepository: RewardRepository by lazy { RewardRepository(api) }

    fun init(context: Context) {
        tokenStorage = TokenStorage(context.applicationContext)
        cookieJar = PersistentCookieJar(context.applicationContext)
        appPreferences = AppPreferences(context.applicationContext)
        ServerConfig.init(appPreferences)
    }
}
