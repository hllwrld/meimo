package com.stx.meimo.util

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("meimo_app_prefs", Context.MODE_PRIVATE)

    var hideImages: Boolean
        get() = prefs.getBoolean(KEY_HIDE_IMAGES, false)
        set(value) { prefs.edit().putBoolean(KEY_HIDE_IMAGES, value).apply() }

    var serverDomain: String
        get() = prefs.getString(KEY_SERVER_DOMAIN, ServerConfig.DEFAULT_DOMAIN) ?: ServerConfig.DEFAULT_DOMAIN
        set(value) { prefs.edit().putString(KEY_SERVER_DOMAIN, value).apply() }

    var customDomains: Set<String>
        get() = prefs.getStringSet(KEY_CUSTOM_DOMAINS, emptySet()) ?: emptySet()
        set(value) { prefs.edit().putStringSet(KEY_CUSTOM_DOMAINS, value).apply() }

    companion object {
        private const val KEY_HIDE_IMAGES = "hide_images"
        private const val KEY_SERVER_DOMAIN = "server_domain"
        private const val KEY_CUSTOM_DOMAINS = "custom_domains"
    }
}
