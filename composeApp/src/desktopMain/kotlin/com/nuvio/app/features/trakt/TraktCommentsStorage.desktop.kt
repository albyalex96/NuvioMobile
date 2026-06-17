package com.nuvio.app.features.trakt

import com.nuvio.app.core.storage.DesktopStorage
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal actual object TraktCommentsStorage {
    private val store = DesktopStorage.store("nuvio_trakt_comments")

    actual fun loadEnabled(): Boolean? =
        if (store.contains("comments_enabled")) store.getBoolean("comments_enabled") else null

    actual fun saveEnabled(enabled: Boolean) {
        store.putBoolean("comments_enabled", enabled)
    }

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadEnabled()?.let { put("comments_enabled", it) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        (payload["comments_enabled"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull()?.let(::saveEnabled)
    }
}
