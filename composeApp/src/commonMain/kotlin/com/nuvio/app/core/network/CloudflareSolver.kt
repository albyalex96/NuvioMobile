package com.nuvio.app.core.network

import com.nuvio.app.features.addons.RawHttpResponse

expect object CloudflareSolver {
    suspend fun solve(url: String): Boolean
    fun getCookies(host: String): Map<String, String>
    fun getWebViewUserAgent(): String?
    fun clear()
}

fun isCloudflareChallenge(response: RawHttpResponse): Boolean {
    if (response.status != 403 && response.status != 503) return false
    val serverHeader = response.headers["server"]?.lowercase() ?: ""
    if (serverHeader.contains("cloudflare")) return true
    val bodyLower = response.body.lowercase()
    return bodyLower.contains("just a moment") ||
            bodyLower.contains("__cf_chl") ||
            bodyLower.contains("cf-ray") ||
            bodyLower.contains("checking your browser")
}
