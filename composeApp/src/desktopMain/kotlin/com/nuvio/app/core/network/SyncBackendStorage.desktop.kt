package com.nuvio.app.core.network

import com.nuvio.app.core.storage.DesktopStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.Proxy

internal actual object SyncBackendStorage {
    private const val KEY_SELECTION_PAYLOAD = "nuvio_sync_backend_selection_payload_v1"

    private val store = DesktopStorage.store("nuvio_sync_backend")

    actual fun loadSelectionPayload(): String? =
        store.getString(KEY_SELECTION_PAYLOAD)

    actual fun saveSelectionPayload(payload: String) {
        store.putString(KEY_SELECTION_PAYLOAD, payload)
    }
}

internal actual suspend fun fetchSyncBackendManifestText(url: String): String =
    withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection(Proxy.NO_PROXY) as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.instanceFollowRedirects = true
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        try {
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                error("Sync backend manifest request failed with HTTP $responseCode")
            }
            connection.inputStream.bufferedReader().readText().takeIf { it.isNotBlank() }
                ?: error("Sync backend manifest response was empty")
        } finally {
            connection.disconnect()
        }
    }
