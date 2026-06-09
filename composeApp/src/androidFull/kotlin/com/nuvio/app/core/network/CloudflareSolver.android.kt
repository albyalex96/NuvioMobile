package com.nuvio.app.core.network

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.http.SslError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

actual object CloudflareSolver {
    private val savedCookies = ConcurrentHashMap<String, Map<String, String>>()
    @Volatile
    private var webViewUserAgent: String? = null
    private var context: Context? = null

    fun initialize(appContext: Context) {
        context = appContext
        CookieManager.getInstance().removeAllCookies(null)
    }

    actual fun getWebViewUserAgent(): String? = webViewUserAgent

    actual fun getCookies(host: String): Map<String, String> =
        savedCookies[host] ?: emptyMap()

    actual fun clear() {
        savedCookies.clear()
        webViewUserAgent = null
    }

    actual suspend fun solve(url: String): Boolean = withContext(Dispatchers.Main) {
        val ctx = context ?: return@withContext false
        val deferred = CompletableDeferred<Boolean>()
        var webView: WebView? = null

        try {
            webView = WebView(ctx.applicationContext).apply {
                @SuppressLint("SetJavaScriptEnabled")
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.blockNetworkImage = true
                settings.builtInZoomControls = false
                settings.displayZoomControls = false

                webViewUserAgent = settings.userAgentString

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        url?.let { checkAndExtractCookies(it) }?.let { solved ->
                            if (solved) deferred.complete(true)
                        }
                    }

                    @SuppressLint("WebViewClientOnReceivedSslError")
                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?,
                    ) {
                        handler?.proceed()
                    }

                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? {
                        return if (shouldBlockResource(request.url.toString())) {
                            WebResourceResponse("image/png", null, null)
                        } else {
                            super.shouldInterceptRequest(view, request)
                        }
                    }
                }
                loadUrl(url)
            }

            withTimeout(60_000L) {
                deferred.await()
            }
        } catch (_: Exception) {
            deferred.complete(false)
        } finally {
            webView?.stopLoading()
            webView?.destroy()
        }
    }

    private fun checkAndExtractCookies(url: String): Boolean {
        val host = URI(url).host ?: return false
        val cookie = CookieManager.getInstance().getCookie(url) ?: return false
        return if (cookie.contains("cf_clearance")) {
            savedCookies[host] = parseCookieMap(cookie)
            true
        } else false
    }

    private fun shouldBlockResource(url: String): Boolean {
        val lower = url.lowercase()
        val blacklisted = listOf(
            ".jpg", ".png", ".webp", ".jpeg", ".webm", ".mp4", ".mp3",
            ".gifv", ".flv", ".asf", ".mov", ".mng", ".mkv", ".ogg", ".avi",
            ".wav", ".woff2", ".woff", ".ttf", ".css", ".vtt", ".srt", ".ts",
            ".gif", "wss://", ".ico",
        )
        return blacklisted.any { lower.contains(it) }
    }
}

private fun parseCookieMap(cookie: String): Map<String, String> =
    cookie.split(";").associate {
        val split = it.split("=", limit = 2)
        (split.getOrNull(0)?.trim() ?: "") to (split.getOrNull(1)?.trim() ?: "")
    }.filter { it.key.isNotBlank() && it.value.isNotBlank() }
