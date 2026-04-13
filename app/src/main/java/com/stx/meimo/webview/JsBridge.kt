package com.stx.meimo.webview

import android.util.Log
import android.webkit.JavascriptInterface
import com.stx.meimo.log.ApiLogger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JsBridge {
    companion object {
        const val NAME = "MeimoBridge"
        private const val TAG = "JsBridge"

        val FETCH_HOOK_JS = """
            (function() {
                if (window.__meimo_hooked) return;
                window.__meimo_hooked = true;

                // Override navigator.userAgentData to mask WebView
                try {
                    var brands = [
                        {brand: "Chromium", version: "131"},
                        {brand: "Google Chrome", version: "131"},
                        {brand: "Not-A.Brand", version: "24"}
                    ];
                    var fullBrands = [
                        {brand: "Chromium", version: "131.0.0.0"},
                        {brand: "Google Chrome", version: "131.0.0.0"},
                        {brand: "Not-A.Brand", version: "24.0.0.0"}
                    ];
                    Object.defineProperty(navigator, 'userAgentData', {
                        get: function() {
                            return {
                                brands: brands,
                                mobile: true,
                                platform: "Android",
                                getHighEntropyValues: function(hints) {
                                    return Promise.resolve({
                                        brands: brands,
                                        fullVersionList: fullBrands,
                                        mobile: true,
                                        platform: "Android",
                                        platformVersion: "14.0.0",
                                        architecture: "arm",
                                        model: "Pixel 8 Pro",
                                        uaFullVersion: "131.0.0.0"
                                    });
                                }
                            };
                        }
                    });
                } catch(e) {}

                // Hook fetch
                var origFetch = window.fetch;
                window.fetch = function(input, init) {
                    var url = (typeof input === 'string') ? input : input.url;
                    var method = (init && init.method) ? init.method : 'GET';
                    var headers = {};
                    if (init && init.headers) {
                        if (init.headers instanceof Headers) {
                            init.headers.forEach(function(v, k) { headers[k] = v; });
                        } else {
                            headers = init.headers;
                        }
                    }
                    var body = (init && init.body) ? String(init.body).substring(0, 4096) : null;

                    var reqInfo = JSON.stringify({
                        type: 'fetch',
                        url: url,
                        method: method,
                        headers: headers,
                        body: body,
                        pageUrl: location.href
                    });

                    return origFetch.apply(this, arguments).then(function(response) {
                        var cloned = response.clone();
                        cloned.text().then(function(text) {
                            try {
                                window.MeimoBridge.onApiCall(
                                    'fetch',
                                    url,
                                    method,
                                    JSON.stringify(headers),
                                    body || '',
                                    response.status,
                                    text.substring(0, 8192),
                                    location.href
                                );
                            } catch(e) {}
                        });
                        return response;
                    });
                };

                // Hook XMLHttpRequest
                var origOpen = XMLHttpRequest.prototype.open;
                var origSend = XMLHttpRequest.prototype.send;
                var origSetHeader = XMLHttpRequest.prototype.setRequestHeader;

                XMLHttpRequest.prototype.open = function(method, url) {
                    this._meimo_method = method;
                    this._meimo_url = url;
                    this._meimo_headers = {};
                    return origOpen.apply(this, arguments);
                };

                XMLHttpRequest.prototype.setRequestHeader = function(key, value) {
                    if (this._meimo_headers) this._meimo_headers[key] = value;
                    return origSetHeader.apply(this, arguments);
                };

                XMLHttpRequest.prototype.send = function(body) {
                    var self = this;
                    this.addEventListener('load', function() {
                        try {
                            window.MeimoBridge.onApiCall(
                                'xhr',
                                self._meimo_url || '',
                                self._meimo_method || 'GET',
                                JSON.stringify(self._meimo_headers || {}),
                                body ? String(body).substring(0, 4096) : '',
                                self.status,
                                self.responseText ? self.responseText.substring(0, 8192) : '',
                                location.href
                            );
                        } catch(e) {}
                    });
                    return origSend.apply(this, arguments);
                };

                console.log('[Meimo] API hooks installed');
            })();
        """.trimIndent()

        val DOM_CAPTURE_JS = """
            (function() {
                function captureDOM(root, depth) {
                    if (depth > 6) return null;
                    var el = root;
                    var result = {
                        tag: el.tagName ? el.tagName.toLowerCase() : '#text',
                        id: el.id || undefined,
                        classes: el.className && typeof el.className === 'string' ? el.className.split(' ').filter(Boolean).slice(0, 5) : undefined,
                        text: undefined,
                        href: el.href || undefined,
                        src: el.src || undefined,
                        type: el.type || undefined,
                        placeholder: el.placeholder || undefined,
                        children: []
                    };
                    if (el.childNodes.length === 1 && el.childNodes[0].nodeType === 3) {
                        var t = el.childNodes[0].textContent.trim();
                        if (t.length > 0 && t.length < 200) result.text = t;
                    }
                    for (var i = 0; i < Math.min(el.children.length, 30); i++) {
                        var child = captureDOM(el.children[i], depth + 1);
                        if (child) result.children.push(child);
                    }
                    if (result.children.length === 0) delete result.children;
                    return result;
                }
                var dom = captureDOM(document.body, 0);
                window.MeimoBridge.onDomCaptured(location.href, JSON.stringify(dom));
            })();
        """.trimIndent()
    }

    @JavascriptInterface
    fun onApiCall(
        type: String,
        url: String,
        method: String,
        headersJson: String,
        body: String,
        status: Int,
        responseBody: String,
        pageUrl: String
    ) {
        val ts = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).format(Date())
        val headers = try {
            @Suppress("UNCHECKED_CAST")
            com.google.gson.Gson().fromJson(headersJson, Map::class.java) as? Map<String, String>
        } catch (_: Exception) { null }

        ApiLogger.logRequest(
            ApiLogger.LogEntry(
                timestamp = ts,
                type = type,
                url = url,
                method = method,
                requestHeaders = headers,
                requestBody = body.ifEmpty { null },
                responseStatus = status,
                responseBody = responseBody.ifEmpty { null },
                pageUrl = pageUrl
            )
        )
    }

    @JavascriptInterface
    fun onDomCaptured(pageUrl: String, domJson: String) {
        Log.d(TAG, "DOM captured for: $pageUrl (${domJson.length} chars)")
        ApiLogger.logDom(pageUrl, domJson)
    }
}
