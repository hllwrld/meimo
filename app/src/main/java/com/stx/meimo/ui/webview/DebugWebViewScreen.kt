package com.stx.meimo.ui.webview

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.stx.meimo.log.ApiLogger
import com.stx.meimo.ui.component.LocalHideImages
import com.stx.meimo.util.ServerConfig
import com.stx.meimo.webview.createMeimoWebView
import com.stx.meimo.webview.hideImagesEnabled

@Composable
fun DebugWebViewScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val hideImages = LocalHideImages.current
    var progress by remember { mutableIntStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    // Sync the global flag used by shouldInterceptRequest
    LaunchedEffect(hideImages) {
        hideImagesEnabled = hideImages
        // Reload current page so blocked/unblocked images take effect
        webView?.post { webView?.reload() }
    }

    DisposableEffect(Unit) {
        ApiLogger.init(context)
        hideImagesEnabled = hideImages
        onDispose { hideImagesEnabled = false }
    }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
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
                    wv.loadUrl(ServerConfig.webUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Progress bar at top
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
