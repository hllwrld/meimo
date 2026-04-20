package com.stx.meimo.webview

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.stx.meimo.log.ApiLogger
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val CHROME_UA = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) " +
    "AppleWebKit/537.36 (KHTML, like Gecko) " +
    "Chrome/131.0.0.0 Mobile Safari/537.36"

private val CLIENT_HINTS_OVERRIDE = mapOf(
    "sec-ch-ua" to "\"Chromium\";v=\"131\", \"Google Chrome\";v=\"131\", \"Not-A.Brand\";v=\"24\"",
    "sec-ch-ua-mobile" to "?1",
    "sec-ch-ua-platform" to "\"Android\"",
    "sec-ch-ua-full-version-list" to "\"Chromium\";v=\"131.0.0.0\", \"Google Chrome\";v=\"131.0.0.0\", \"Not-A.Brand\";v=\"24.0.0.0\"",
)

private val okClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .followRedirects(true)
    .followSslRedirects(true)
    .build()

private val SKIP_HEADERS = setOf(
    "host", "connection", "upgrade", "keep-alive",
    "transfer-encoding", "te", "trailer", "proxy-authorization",
    "proxy-connection"
)

@SuppressLint("SetJavaScriptEnabled")
/** When true, CDN image requests return a 1x1 gray pixel instead of the real image. */
@Volatile
var hideImagesEnabled = false

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "avif", "svg", "bmp", "ico")

private val GRAY_PIXEL_PNG: ByteArray by lazy {
    // 1x1 #BDBDBD PNG
    val bmp = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
    bmp.setPixel(0, 0, android.graphics.Color.parseColor("#BDBDBD"))
    val out = java.io.ByteArrayOutputStream()
    bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
    bmp.recycle()
    out.toByteArray()
}

fun createMeimoWebView(
    context: Context,
    onProgressChanged: (Int) -> Unit,
    onTitleChanged: (String) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: ((WebView, String?) -> Unit)? = null
): WebView {
    WebView.setWebContentsDebuggingEnabled(true)
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val jsBridge = JsBridge()
        addJavascriptInterface(jsBridge, JsBridge.NAME)

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            allowContentAccess = true
            allowFileAccess = true
            useWideViewPort = false
            loadWithOverviewMode = false
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            userAgentString = CHROME_UA
        }

        val webView = this
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                view?.evaluateJavascript(JsBridge.FETCH_HOOK_JS, null)
                url?.let { onPageStarted(it) }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                CookieManager.getInstance().flush()
                Log.i("MeimoWebView", "Page loaded: $url")

                view?.evaluateJavascript(JsBridge.FETCH_HOOK_JS, null)
                view?.postDelayed({
                    view.evaluateJavascript(JsBridge.DOM_CAPTURE_JS, null)
                }, 2000)
                if (view != null) onPageFinished?.invoke(view, url)
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val req = request ?: return null
                val url = req.url.toString()
                val method = req.method ?: "GET"

                // Block CDN/background images based on current server domain
                if (hideImagesEnabled) {
                    val lower = url.lowercase()
                    val cdnHost = "r2.${com.stx.meimo.util.ServerConfig.currentDomain}".lowercase()
                    val isCdnImage = lower.contains(cdnHost) || lower.contains("/uploads/")
                    if (isCdnImage) {
                        return WebResourceResponse(
                            "image/png", "UTF-8",
                            ByteArrayInputStream(GRAY_PIXEL_PNG)
                        )
                    }
                }

                // Log all API requests for analysis
                val ts = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).format(Date())
                if (url.contains("/api/") || url.contains("/graphql") ||
                    url.contains("/auth") || url.contains("/login") ||
                    url.contains("/generate") || url.contains("/models") ||
                    url.contains("/gallery") || url.contains("/user")
                ) {
                    ApiLogger.logRequest(
                        ApiLogger.LogEntry(
                            timestamp = ts,
                            type = "intercept",
                            url = url,
                            method = method,
                            requestHeaders = req.requestHeaders?.toMap()
                        )
                    )
                }

                // TODO: re-enable proxy after confirming base WebView works
                return null

                @Suppress("UNREACHABLE_CODE")
                if (!url.contains("sexyai.top") || !method.equals("GET", true)) {
                    return null
                }

                try {
                    val builder = Request.Builder().url(url).get()

                    // Copy original headers, replacing Client Hints
                    req.requestHeaders?.forEach { (k, v) ->
                        val lower = k.lowercase()
                        if (lower !in SKIP_HEADERS && !lower.startsWith("sec-ch-ua") && !lower.equals("user-agent", true)) {
                            builder.addHeader(k, v)
                        }
                    }
                    builder.header("User-Agent", CHROME_UA)
                    CLIENT_HINTS_OVERRIDE.forEach { (k, v) -> builder.header(k, v) }

                    val cookie = CookieManager.getInstance().getCookie(url)
                    if (cookie != null) builder.header("Cookie", cookie)

                    val response = okClient.newCall(builder.build()).execute()

                    response.headers("Set-Cookie").forEach { setCookie ->
                        CookieManager.getInstance().setCookie(url, setCookie)
                    }

                    val respContentType = response.header("Content-Type") ?: "text/html"
                    val mimeType = respContentType.split(";").first().trim()
                    val encoding = if (respContentType.contains("charset=", ignoreCase = true)) {
                        respContentType.substringAfter("charset=", "UTF-8").trim()
                    } else "UTF-8"

                    val respBody = response.body?.bytes() ?: ByteArray(0)

                    val responseHeaders = mutableMapOf<String, String>()
                    response.headers.forEach { (k, v) ->
                        val lower = k.lowercase()
                        if (lower != "content-encoding" && lower != "transfer-encoding" && lower != "content-length") {
                            responseHeaders[k] = v
                        }
                    }

                    return WebResourceResponse(
                        mimeType,
                        encoding,
                        response.code,
                        response.message.ifEmpty { "OK" },
                        responseHeaders,
                        ByteArrayInputStream(respBody)
                    )
                } catch (e: Exception) {
                    Log.e("MeimoWebView", "Proxy failed for $url", e)
                    return null
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.contains("sexyai.top") || url.startsWith("https://")) {
                    return false
                }
                return true
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                onProgressChanged(newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                title?.let { onTitleChanged(it) }
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    val level = when (it.messageLevel()) {
                        ConsoleMessage.MessageLevel.ERROR -> "E"
                        ConsoleMessage.MessageLevel.WARNING -> "W"
                        else -> "I"
                    }
                    Log.println(
                        when (level) { "E" -> Log.ERROR; "W" -> Log.WARN; else -> Log.INFO },
                        "WebConsole",
                        "[${level}] ${it.message()} (${it.sourceId()}:${it.lineNumber()})"
                    )
                }
                return true
            }
        }

        setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
                    setTitle(filename)
                    setDescription("Downloading...")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Meimo/$filename")
                    addRequestHeader("User-Agent", userAgent)
                    val cookie = CookieManager.getInstance().getCookie(url)
                    if (cookie != null) addRequestHeader("Cookie", cookie)
                }
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("MeimoWebView", "Download failed", e)
                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
