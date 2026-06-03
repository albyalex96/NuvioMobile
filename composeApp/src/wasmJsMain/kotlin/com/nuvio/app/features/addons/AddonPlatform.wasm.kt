package com.nuvio.app.features.addons

import com.nuvio.app.WebStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess

actual object AddonStorage {
    private const val addonUrlsKey = "nuvio_installed_manifest_urls"
    private const val addonEnabledStatesKey = "nuvio_installed_manifest_enabled_states"

    actual fun loadInstalledAddonUrls(profileId: Int): List<String> =
        WebStorage.getString("${addonUrlsKey}_$profileId")
            .orEmpty()
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

    actual fun saveInstalledAddonUrls(profileId: Int, urls: List<String>) {
        WebStorage.setString("${addonUrlsKey}_$profileId", urls.joinToString("\n"))
    }

    actual fun loadAddonEnabledStates(profileId: Int): Map<String, Boolean> {
        val raw = WebStorage.getString("${addonEnabledStatesKey}_$profileId") ?: return emptyMap()
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    val value = parts[1].trim().toBooleanStrictOrNull()
                    if (value != null) parts[0].trim() to value else null
                } else null
            }
            .toMap()
    }

    actual fun saveAddonEnabledStates(profileId: Int, states: Map<String, Boolean>) {
        val raw = states.entries.joinToString("\n") { "${it.key}=${it.value}" }
        WebStorage.setString("${addonEnabledStatesKey}_$profileId", raw)
    }
}

private val httpClient = HttpClient(Js) {
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 15_000
    }
}

actual suspend fun httpGetText(url: String): String =
    httpClient.get(url).bodyAsText()

actual suspend fun httpPostJson(url: String, body: String): String =
    httpClient.post(url) {
        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(body)
    }.bodyAsText()

actual suspend fun httpGetTextWithHeaders(
    url: String,
    headers: Map<String, String>,
): String = httpClient.get(url) {
    headers.forEach { (key, value) -> header(key, value) }
}.bodyAsText()

actual suspend fun httpPostJsonWithHeaders(
    url: String,
    body: String,
    headers: Map<String, String>,
): String = httpClient.post(url) {
    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    setBody(body)
    headers.forEach { (key, value) -> header(key, value) }
}.bodyAsText()

actual suspend fun httpRequestRaw(
    method: String,
    url: String,
    headers: Map<String, String>,
    body: String,
    followRedirects: Boolean,
): RawHttpResponse {
    val httpMethod = HttpMethod.parse(method)
    val response = httpClient.request(url) {
        this.method = httpMethod
        if (body.isNotEmpty()) setBody(body)
        headers.forEach { (key, value) -> header(key, value) }
    }
    val responseHeaders = mutableMapOf<String, String>()
    response.headers.forEach { name, values ->
        if (values.isNotEmpty()) responseHeaders[name] = values.first()
    }
    return RawHttpResponse(
        status = response.status.value,
        statusText = response.status.description,
        url = url,
        body = response.bodyAsText(),
        headers = responseHeaders,
    )
}
