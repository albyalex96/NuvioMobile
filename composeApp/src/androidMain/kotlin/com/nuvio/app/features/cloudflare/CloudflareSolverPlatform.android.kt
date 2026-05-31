package com.nuvio.app.features.cloudflare

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private val CLOUDFLARE_COOKIE_KEYS = listOf("cf_clearance", "__cf_bm", "cf_chl_2", "cf_chl_prog")

private fun hasCloudflareCookie(url: String): Boolean {
    return try {
        val uri = Uri.parse(url)
        val host = uri.host ?: return false
        val baseUrl = "${uri.scheme}://$host"
        val cookies = CookieManager.getInstance().getCookie(baseUrl) ?: return false
        CLOUDFLARE_COOKIE_KEYS.any { key -> cookies.contains(key) }
    } catch (e: Exception) {
        false
    }
}

@Composable
actual fun CloudflareSolverWebView(
    url: String,
    onResolved: () -> Unit,
    modifier: Modifier
) {
    val handler = remember { Handler(Looper.getMainLooper()) }
    val resolved = remember { booleanArrayOf(false) }

    DisposableEffect(url) {
        resolved[0] = false
        onDispose { resolved[0] = true }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.userAgentString =
                    "Mozilla/5.0 (Linux; Android 10; SM-G981B) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                val cookiePoller = object : Runnable {
                    override fun run() {
                        if (resolved[0]) return
                        if (hasCloudflareCookie(url)) {
                            resolved[0] = true
                            CookieManager.getInstance().flush()
                            onResolved()
                            return
                        }
                        handler.postDelayed(this, 800)
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                        super.onPageFinished(view, finishedUrl)
                        if (!resolved[0]) {
                            val checkUrl = finishedUrl ?: url
                            if (hasCloudflareCookie(checkUrl) || hasCloudflareCookie(url)) {
                                resolved[0] = true
                                CookieManager.getInstance().flush()
                                handler.removeCallbacks(cookiePoller)
                                onResolved()
                            }
                        }
                    }

                    override fun onPageStarted(view: WebView?, startedUrl: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, startedUrl, favicon)
                        if (!resolved[0]) {
                            val checkUrl = startedUrl ?: url
                            if (hasCloudflareCookie(checkUrl) || hasCloudflareCookie(url)) {
                                resolved[0] = true
                                CookieManager.getInstance().flush()
                                handler.removeCallbacks(cookiePoller)
                                onResolved()
                            }
                        }
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return false
                    }
                }

                handler.postDelayed(cookiePoller, 1000)
                loadUrl(url)
            }
        }
    )
}
