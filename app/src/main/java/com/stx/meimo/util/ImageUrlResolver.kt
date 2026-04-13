package com.stx.meimo.util

private const val CDN_BASE = "https://r2.sexyai.top"

fun resolveImageUrl(path: String?): String? {
    if (path.isNullOrBlank() || path == "/") return null
    if (path.startsWith("http://") || path.startsWith("https://")) return path
    return CDN_BASE + path
}
