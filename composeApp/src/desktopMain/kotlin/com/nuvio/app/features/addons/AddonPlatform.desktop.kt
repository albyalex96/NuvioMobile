package com.nuvio.app.features.addons

import com.nuvio.app.core.storage.DesktopStorage
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual object AddonStorage {
    private val store = DesktopStorage.store("nuvio_addon_storage")
    private val json = Json { ignoreUnknownKeys = true }

    actual fun loadInstalledAddonUrls(profileId: Int): List<String> {
        return store.getString("installed_addon_urls_$profileId")?.let { payload ->
            runCatching { json.decodeFromString<List<String>>(payload) }.getOrNull()
        } ?: emptyList()
    }

    actual fun saveInstalledAddonUrls(profileId: Int, urls: List<String>) {
        store.putString("installed_addon_urls_$profileId", json.encodeToString(urls))
    }

    actual fun loadAddonEnabledStates(profileId: Int): Map<String, Boolean> {
        return store.getString("addon_enabled_states_$profileId")?.let { payload ->
            runCatching { json.decodeFromString<Map<String, Boolean>>(payload) }.getOrNull()
        } ?: emptyMap()
    }

    actual fun saveAddonEnabledStates(profileId: Int, states: Map<String, Boolean>) {
        store.putString("addon_enabled_states_$profileId", json.encodeToString(states))
    }
}

private const val MAX_BODY_BYTES = 1024 * 1024
private const val TRUNCATION_SUFFIX = "\n...[truncated]"

private suspend fun executeTextRequest(
    method: String,
    url: String,
    headers: Map<String, String> = emptyMap(),
    body: String = "",
): String = withContext(Dispatchers.IO) {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.requestMethod = method.uppercase()
    conn.connectTimeout = 60000
    conn.readTimeout = 60000
    conn.instanceFollowRedirects = true
    headers.forEach { (key, value) -> conn.setRequestProperty(key, value) }
    if (body.isNotEmpty()) {
        conn.doOutput = true
        OutputStreamWriter(conn.outputStream).use { it.write(body) }
    }
    val status = conn.responseCode
    val responseBody = try {
        conn.inputStream.bufferedReader().readText()
    } catch (_: Exception) {
        conn.errorStream?.bufferedReader()?.readText() ?: ""
    }
    if (status !in 200..299) {
        throw IllegalStateException("HTTP $status: $responseBody")
    }
    if (responseBody.isBlank()) {
        throw IllegalStateException("Empty response body")
    }
    responseBody
}

actual suspend fun httpGetText(url: String): String =
    executeTextRequest("GET", url, mapOf("Accept" to "application/json"))

actual suspend fun httpPostJson(url: String, body: String): String =
    executeTextRequest("POST", url, mapOf("Accept" to "application/json", "Content-Type" to "application/json"), body)

actual suspend fun httpGetTextWithHeaders(url: String, headers: Map<String, String>): String =
    executeTextRequest("GET", url, mapOf("Accept" to "application/json") + headers)

actual suspend fun httpPostJsonWithHeaders(url: String, body: String, headers: Map<String, String>): String =
    executeTextRequest("POST", url, mapOf("Accept" to "application/json", "Content-Type" to "application/json") + headers, body)

actual suspend fun httpGetBytesWithHeaders(
    url: String,
    headers: Map<String, String>,
): ByteArray = withContext(Dispatchers.IO) {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = 60000
    conn.readTimeout = 60000
    headers.forEach { (key, value) -> conn.setRequestProperty(key, value) }
    try {
        conn.inputStream.readBytes()
    } finally {
        conn.disconnect()
    }
}

actual suspend fun httpRequestRaw(
    method: String,
    url: String,
    headers: Map<String, String>,
    body: String,
    followRedirects: Boolean,
): RawHttpResponse = withContext(Dispatchers.IO) {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.requestMethod = method.uppercase()
    conn.connectTimeout = 60000
    conn.readTimeout = 60000
    conn.instanceFollowRedirects = followRedirects
    headers.forEach { (key, value) -> conn.setRequestProperty(key, value) }
    if (body.isNotEmpty()) {
        conn.doOutput = true
        OutputStreamWriter(conn.outputStream).use { it.write(body) }
    }
    val status = conn.responseCode
    val responseHeaders = conn.headerFields
        .filterKeys { it != null }
        .mapKeys { (key, _) -> key!!.lowercase() }
        .mapValues { (_, values) -> values.joinToString(",") }
    val rawBody = try {
        conn.inputStream.bufferedReader().readText()
    } catch (_: Exception) {
        conn.errorStream?.bufferedReader()?.readText() ?: ""
    }
    val bodyLimited = if (rawBody.length > MAX_BODY_BYTES) {
        rawBody.take(MAX_BODY_BYTES) + TRUNCATION_SUFFIX
    } else {
        rawBody
    }
    RawHttpResponse(
        status = status,
        statusText = conn.responseMessage ?: "",
        url = conn.url.toString(),
        body = bodyLimited,
        headers = responseHeaders,
    )
}
