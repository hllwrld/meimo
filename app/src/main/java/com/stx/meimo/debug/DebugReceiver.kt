package com.stx.meimo.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.webkit.CookieManager
import com.stx.meimo.di.AppModule
import com.stx.meimo.util.generateIdempotencyKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * ADB debug receiver. Usage:
 *
 * # Dump WebView cookies
 * adb shell am broadcast -a com.stx.meimo.DEBUG --es cmd cookies
 *
 * # Native login
 * adb shell am broadcast -a com.stx.meimo.DEBUG --es cmd login --es user EMAIL --es pass PASSWORD
 *
 * # Test API call (uses current auth state)
 * adb shell am broadcast -a com.stx.meimo.DEBUG --es cmd api --es endpoint "message/history/page" --es body '{"page":1,"roleId":"15027","size":50}'
 *
 * # Raw API call with specific auth header
 * adb shell am broadcast -a com.stx.meimo.DEBUG --es cmd raw_api --es endpoint "message/history/page" --es body '{"page":1,"roleId":"15027","size":50}' --es auth "FAKE_TOKEN_HERE"
 *
 * # Dump all auth state
 * adb shell am broadcast -a com.stx.meimo.DEBUG --es cmd auth_state
 *
 * # Test model list via Retrofit (same path as ChatViewModel)
 * adb shell am broadcast -a com.stx.meimo.DEBUG --es cmd test_models
 *
 * # Test chat send (SSE)
 * adb shell am broadcast -a com.stx.meimo.DEBUG --es cmd test_chat --es roleId "15027" --es msg "你好" --es modelId "1"
 */
class DebugReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MeimoDebug"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val cmd = intent.getStringExtra("cmd") ?: "help"
        Log.i(TAG, "=== DEBUG CMD: $cmd ===")

        when (cmd) {
            "cookies" -> dumpCookies()
            "auth_state" -> dumpAuthState()
            "login" -> {
                val user = intent.getStringExtra("user") ?: ""
                val pass = intent.getStringExtra("pass") ?: ""
                nativeLogin(user, pass)
            }
            "api" -> {
                val endpoint = intent.getStringExtra("endpoint") ?: ""
                val body = intent.getStringExtra("body") ?: "{}"
                testApiCall(endpoint, body)
            }
            "raw_api" -> {
                val endpoint = intent.getStringExtra("endpoint") ?: ""
                val body = intent.getStringExtra("body") ?: "{}"
                val auth = intent.getStringExtra("auth") ?: ""
                rawApiCall(endpoint, body, auth)
            }
            "test_models" -> testModelsViaRetrofit()
            "test_chat" -> {
                val roleId = intent.getStringExtra("roleId") ?: "15027"
                val msg = intent.getStringExtra("msg") ?: "你好"
                val modelId = intent.getStringExtra("modelId") ?: "1"
                testChatSend(roleId, msg, modelId.toIntOrNull() ?: 1)
            }
            else -> Log.i(TAG, "Commands: cookies, auth_state, login, api, raw_api, test_models, test_chat")
        }
    }

    private fun dumpCookies() {
        val cookies = try {
            CookieManager.getInstance().getCookie("https://sexyai.top")
        } catch (e: Exception) {
            "ERROR: ${e.message}"
        }
        Log.i(TAG, "WebView cookies for sexyai.top:")
        if (cookies.isNullOrBlank()) {
            Log.i(TAG, "  (none)")
        } else {
            cookies.split(";").forEach { cookie ->
                val trimmed = cookie.trim()
                val name = trimmed.substringBefore("=")
                val value = trimmed.substringAfter("=")
                val display = if (value.length > 40) "${value.take(40)}..." else value
                Log.i(TAG, "  $name = $display")
            }
        }
    }

    private fun dumpAuthState() {
        val ts = AppModule.tokenStorage
        Log.i(TAG, "Auth state:")
        Log.i(TAG, "  isLoggedIn: ${ts.isLoggedIn}")
        Log.i(TAG, "  storedToken: ${ts.getToken()?.take(60) ?: "(null)"}")

        // Check persistent cookie jar
        val url = okhttp3.HttpUrl.Builder().scheme("https").host("sexyai.top").build()
        val cookies = AppModule.cookieJar.loadForRequest(url)
        cookies.forEach { cookie ->
            val display = if (cookie.value.length > 60) "${cookie.value.take(60)}..." else cookie.value
            Log.i(TAG, "  cookie: ${cookie.name}=$display")
        }
        if (cookies.isEmpty()) {
            Log.i(TAG, "  cookies: (none)")
        }
    }

    private fun nativeLogin(user: String, pass: String) {
        if (user.isBlank() || pass.isBlank()) {
            Log.e(TAG, "Usage: --es user EMAIL --es pass PASSWORD")
            return
        }
        Log.i(TAG, "Logging in as: $user")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Use AuthRepository for proper login flow (saves token + sets logged in)
                val result = AppModule.authRepository.login(user, pass)
                result.onSuccess { userDto ->
                    Log.i(TAG, "Login SUCCESS: nickname=${userDto.nickname}, id=${userDto.id}")
                    Log.i(TAG, "  token saved: ${AppModule.tokenStorage.getToken()?.take(80) ?: "(null)"}")
                    Log.i(TAG, "  isLoggedIn: ${AppModule.tokenStorage.isLoggedIn}")

                    // Dump persistent cookies
                    val url = okhttp3.HttpUrl.Builder().scheme("https").host("sexyai.top").build()
                    AppModule.cookieJar.loadForRequest(url).forEach { cookie ->
                        Log.i(TAG, "  cookie: ${cookie.name}=${cookie.value.take(60)}")
                    }
                }.onFailure { e ->
                    Log.e(TAG, "Login FAILED: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login error: ${e.message}", e)
            }
        }
    }

    private fun testApiCall(endpoint: String, body: String) {
        if (endpoint.isBlank()) {
            Log.e(TAG, "Usage: --es endpoint 'message/history/page' --es body '{...}'")
            return
        }
        Log.i(TAG, "API call: $endpoint")
        Log.i(TAG, "Body: $body")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Use the app's OkHttpClient which includes AuthInterceptor
                val request = Request.Builder()
                    .url("https://sexyai.top/api/$endpoint")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = AppModule.okHttpClient.newCall(request).execute()
                Log.i(TAG, "Response status: ${response.code}")

                // Log request headers that were actually sent (after interceptor)
                Log.i(TAG, "Sent headers:")
                response.request.headers.forEach { (name, value) ->
                    val display = if (value.length > 80) "${value.take(80)}..." else value
                    Log.i(TAG, "  $name: $display")
                }

                val respBody = response.body?.string() ?: ""
                Log.i(TAG, "Response body (first 500): ${respBody.take(500)}")
                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "API call failed: ${e.message}", e)
            }
        }
    }

    private fun testModelsViaRetrofit() {
        Log.i(TAG, "Testing model list via Retrofit (same path as ChatViewModel)...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = AppModule.modelRepository.getModels()
                result.onSuccess { models ->
                    Log.i(TAG, "Models loaded: ${models.size} models")
                    models.forEachIndexed { i, m ->
                        Log.i(TAG, "  [$i] id=${m.id} name=${m.name} deduct=${m.deductNum} stream=${m.enableStream}")
                    }
                }.onFailure { e ->
                    Log.e(TAG, "Model load FAILED: ${e.message}", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Model load exception: ${e.message}", e)
            }
        }
    }

    private fun testChatSend(roleId: String, msg: String, modelId: Int) {
        Log.i(TAG, "Testing chat send: roleId=$roleId msg=$msg modelId=$modelId")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = AppModule.chatRepository.sendMessage(roleId.toLong(), msg, modelId)
                result.onSuccess { message ->
                    Log.i(TAG, "Chat reply SUCCESS:")
                    Log.i(TAG, "  id=${message.id} direction=${message.direction}")
                    Log.i(TAG, "  model=${message.model} consumption=${message.consumption}")
                    Log.i(TAG, "  content (first 300): ${message.content?.take(300)}")
                }.onFailure { e ->
                    Log.e(TAG, "Chat send FAILED: ${e.message}", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Chat send exception: ${e.message}", e)
            }
        }
    }

    private fun rawApiCall(endpoint: String, body: String, auth: String) {
        if (endpoint.isBlank()) {
            Log.e(TAG, "Usage: --es endpoint 'message/history/page' --es body '{...}' --es auth 'TOKEN'")
            return
        }
        Log.i(TAG, "Raw API call: $endpoint")
        Log.i(TAG, "Auth: ${auth.take(60)}")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val builder = Request.Builder()
                    .url("https://sexyai.top/api/$endpoint")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .header("Content-Type", "application/json")
                    .header("Lang", "ZH")
                    .header("Idempotency-Key", generateIdempotencyKey())

                if (auth.isNotBlank()) {
                    builder.header("Authorization", auth)
                }

                // Also add WebView cookies
                val cookies = try {
                    CookieManager.getInstance().getCookie("https://sexyai.top")
                } catch (_: Exception) { null }
                if (!cookies.isNullOrBlank()) {
                    builder.header("Cookie", cookies)
                }

                // Use a plain OkHttp client (no interceptor) for raw testing
                val plainClient = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val response = plainClient.newCall(builder.build()).execute()
                Log.i(TAG, "Response status: ${response.code}")
                val respBody = response.body?.string() ?: ""
                Log.i(TAG, "Response body (first 500): ${respBody.take(500)}")
                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "Raw API call failed: ${e.message}", e)
            }
        }
    }
}
