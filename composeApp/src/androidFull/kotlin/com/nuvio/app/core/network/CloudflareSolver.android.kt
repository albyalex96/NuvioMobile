package com.nuvio.app.core.network

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.http.SslError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
        Log.d("CloudflareKiller", "solve() called for URL: $url")
        val deferred = CompletableDeferred<Boolean>()
        var webView: WebView? = null
        val originalHost = URI(url).host ?: ""

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
                        val pageUrl = url ?: return
                        if (tryExtractCookie(pageUrl) || tryExtractCookie(originalHost)) {
                            deferred.complete(true)
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
                while (!deferred.isCompleted) {
                    if (tryExtractCookie(url) || tryExtractCookie(originalHost)) {
                        deferred.complete(true)
                    } else {
                        delay(500)
                    }
                }
                deferred.await()
            }
        } catch (_: Exception) {
            if (!deferred.isCompleted) deferred.complete(false)
            false
        } finally {
            webView?.stopLoading()
            webView?.destroy()
        }
    }

    private fun tryExtractCookie(urlOrHost: String): Boolean {
        val host = if (urlOrHost.startsWith("http")) URI(urlOrHost).host ?: return false
            else urlOrHost

        val cookie = CookieManager.getInstance().getCookie(
            if (urlOrHost.startsWith("http")) urlOrHost else "https://$urlOrHost/"
        ) ?: return false

        return if (cookie.contains("cf_clearance")) {
            savedCookies[host] = parseCookieMap(cookie)
            Log.d("CloudflareKiller", "cf_clearance found for host: $host")
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
