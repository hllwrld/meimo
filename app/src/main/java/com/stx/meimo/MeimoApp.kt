package com.stx.meimo

import android.app.Application
import android.webkit.CookieManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.stx.meimo.di.AppModule

class MeimoApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        CookieManager.getInstance().setAcceptCookie(true)
        AppModule.init(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(AppModule.okHttpClient)
            .crossfade(true)
            .build()
    }
}
