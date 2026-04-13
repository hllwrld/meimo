package com.stx.meimo.ui.chat

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.stx.meimo.log.ApiLogger
import com.stx.meimo.ui.component.LocalHideImages
import com.stx.meimo.webview.createMeimoWebView
import com.stx.meimo.webview.hideImagesEnabled

@Composable
fun ChatWebViewScreen(
    roleId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val hideImages = LocalHideImages.current
    var progress by remember { mutableIntStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val chatUrl = "https://sexyai.top/#/pages/chat/chat?roleId=$roleId"

    LaunchedEffect(hideImages) {
        hideImagesEnabled = hideImages
        webView?.post { webView?.reload() }
    }

    DisposableEffect(Unit) {
        ApiLogger.init(context)
        hideImagesEnabled = hideImages
        onDispose { hideImagesEnabled = false }
    }

    // 返回键直接退出聊天 WebView，不在 SPA 内回退
    BackHandler {
        onBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                createMeimoWebView(
                    context = ctx,
                    onProgressChanged = { progress = it },
                    onTitleChanged = { },
                    onPageStarted = { }
                ).also { wv ->
                    webView = wv
                    wv.loadUrl(chatUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = progress in 1..99,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}
