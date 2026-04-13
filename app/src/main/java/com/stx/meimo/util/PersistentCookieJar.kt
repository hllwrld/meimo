package com.stx.meimo.util

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class PersistentCookieJar(context: Context) : CookieJar {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("cookies", Context.MODE_PRIVATE)
    private val cache = mutableMapOf<String, MutableList<Cookie>>()

    init {
        loadFromDisk()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val existing = cache.getOrPut(host) { mutableListOf() }
        for (cookie in cookies) {
            existing.removeAll { it.name == cookie.name }
            existing.add(cookie)
        }
        persistToDisk(host, existing)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val cookies = cache[host] ?: return emptyList()
        val now = System.currentTimeMillis() / 1000
        val valid = cookies.filter { it.expiresAt / 1000 > now }
        if (valid.size != cookies.size) {
            cache[host] = valid.toMutableList()
            persistToDisk(host, valid)
        }
        return valid
    }

    fun clear() {
        cache.clear()
        prefs.edit().clear().apply()
    }

    private fun persistToDisk(host: String, cookies: List<Cookie>) {
        val serialized = cookies.joinToString("|") { serializeCookie(it) }
        prefs.edit().putString(host, serialized).apply()
    }

    private fun loadFromDisk() {
        for ((host, serialized) in prefs.all) {
            val str = serialized as? String ?: continue
            val cookies = str.split("|").mapNotNull { deserializeCookie(it) }
            if (cookies.isNotEmpty()) {
                cache[host] = cookies.toMutableList()
            }
        }
    }

    private fun serializeCookie(cookie: Cookie): String {
        // Format: name=value;domain;path;expiresAt;secure;httpOnly
        return listOf(
            "${cookie.name}=${cookie.value}",
            cookie.domain,
            cookie.path,
            cookie.expiresAt.toString(),
            if (cookie.secure) "1" else "0",
            if (cookie.httpOnly) "1" else "0"
        ).joinToString(";")
    }

    private fun deserializeCookie(str: String): Cookie? {
        val parts = str.split(";")
        if (parts.size < 6) return null
        val nameValue = parts[0].split("=", limit = 2)
        if (nameValue.size < 2) return null
        return Cookie.Builder()
            .name(nameValue[0])
            .value(nameValue[1])
            .domain(parts[1])
            .path(parts[2])
            .expiresAt(parts[3].toLongOrNull() ?: 0)
            .apply {
                if (parts[4] == "1") secure()
                if (parts[5] == "1") httpOnly()
            }
            .build()
    }
}
