package com.nuvio.app.core.network

import android.content.Context

actual object CloudflareSolver {
    fun initialize(appContext: Context) = Unit

    actual suspend fun solve(url: String): Boolean = false
    actual fun getCookies(host: String): Map<String, String> = emptyMap()
    actual fun getWebViewUserAgent(): String? = null
    actual fun clear() = Unit
}
