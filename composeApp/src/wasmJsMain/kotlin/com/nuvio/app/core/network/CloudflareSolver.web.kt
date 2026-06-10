package com.nuvio.app.core.network

actual object CloudflareSolver {
    actual suspend fun solve(url: String): Boolean = true

    actual fun getCookies(host: String): Map<String, String> = emptyMap()

    actual fun getWebViewUserAgent(): String? = null

    actual fun clear() = Unit
}
