package com.stx.meimo.util

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("meimo_app_prefs", Context.MODE_PRIVATE)

    var hideImages: Boolean
        get() = prefs.getBoolean(KEY_HIDE_IMAGES, false)
        set(value) { prefs.edit().putBoolean(KEY_HIDE_IMAGES, value).apply() }

    companion object {
        private const val KEY_HIDE_IMAGES = "hide_images"
    }
}
