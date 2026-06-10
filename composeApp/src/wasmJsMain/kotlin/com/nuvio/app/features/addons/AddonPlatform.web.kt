package com.nuvio.app.features.addons

import com.nuvio.app.core.platform.WebKeyValueStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal actual object AddonStorage {
    private const val namespace = "nuvio_addons"

    actual fun loadInstalledAddonUrls(profileId: Int): List<String> {
        val raw = WebKeyValueStorage.getString(namespace, "installed_urls_$profileId")
            ?: return emptyList()
        return raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
    }

    actual fun saveInstalledAddonUrls(profileId: Int, urls: List<String>) {
        WebKeyValueStorage.setString(namespace, "installed_urls_$profileId", urls.joinToString("\n"))
    }

    actual fun loadAddonEnabledStates(profileId: Int): Map<String, Boolean> {
        val raw = WebKeyValueStorage.getString(namespace, "enabled_states_$profileId") ?: return emptyMap()
        return runCatching {
            Json.parseToJsonElement(raw).jsonObject.entries.associate { (k, v) ->
                k to v.jsonPrimitive.boolean
            }
        }.getOrDefault(emptyMap())
    }

    actual fun saveAddonEnabledStates(profileId: Int, states: Map<String, Boolean>) {
        val json = buildJsonObject {
            states.forEach { (k, v) -> put(k, v) }
        }
        WebKeyValueStorage.setString(namespace, "enabled_states_$profileId", json.toString())
    }
}

private val addonHttpClient = HttpClient(Js)

actual suspend fun httpGetText(url: String): String =
    addonHttpClient.get(url).bodyAsText()

actual suspend fun httpPostJson(url: String, body: String): String =
    addonHttpClient.post(url) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }.bodyAsText()

actual suspend fun httpGetTextWithHeaders(
    url: String,
    headers: Map<String, String>,
): String =
    addonHttpClient.get(url) {
        appendRequestHeaders(headers)
    }.bodyAsText()

actual suspend fun httpPostJsonWithHeaders(
    url: String,
    body: String,
    headers: Map<String, String>,
): String =
    addonHttpClient.post(url) {
        contentType(ContentType.Application.Json)
        appendRequestHeaders(headers)
        setBody(body)
    }.bodyAsText()

actual suspend fun httpRequestRaw(
    method: String,
    url: String,
    headers: Map<String, String>,
    body: String,
    followRedirects: Boolean,
): RawHttpResponse {
    val response = addonHttpClient.request(url) {
        this.method = HttpMethod.parse(method)
        appendRequestHeaders(headers)
        if (requestAllowsBody(method)) {
            setBody(body)
        }
    }
    return response.toRawHttpResponse()
}

private fun io.ktor.client.request.HttpRequestBuilder.appendRequestHeaders(headers: Map<String, String>) {
    headers.forEach { (key, value) ->
        if (key.isNotBlank() && value.isNotBlank()) {
            header(key, value)
        }
    }
}

private fun requestAllowsBody(method: String): Boolean =
    when (method.uppercase()) {
        "POST", "PUT", "PATCH", "DELETE" -> true
        else -> false
    }

private suspend fun HttpResponse.toRawHttpResponse(): RawHttpResponse =
    RawHttpResponse(
        status = status.value,
        statusText = status.description,
        url = call.request.url.toString(),
        body = bodyAsText(),
        headers = headers.entries().associate { (key, values) -> key to values.joinToString(",") },
    )
