package com.nuvio.app.features.trakt

import com.nuvio.app.core.platform.WebKeyValueStorage
import com.nuvio.app.core.platform.webNowEpochMs
import com.nuvio.app.core.platform.webParseIsoDateTimeToEpochMs
import com.nuvio.app.core.storage.ProfileScopedKey
import com.nuvio.app.core.sync.decodeSyncBoolean
import com.nuvio.app.core.sync.encodeSyncBoolean
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal actual object TraktAuthStorage {
    private const val namespace = "nuvio_trakt_auth"
    private const val payloadKey = "trakt_auth_payload"

    actual fun loadPayload(): String? =
        WebKeyValueStorage.getString(namespace, ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        WebKeyValueStorage.setString(namespace, ProfileScopedKey.of(payloadKey), payload)
    }
}

internal actual object TraktLibraryStorage {
    private const val namespace = "nuvio_trakt_library"
    private const val payloadKey = "trakt_library_payload"

    actual fun loadPayload(): String? =
        WebKeyValueStorage.getString(namespace, ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        WebKeyValueStorage.setString(namespace, ProfileScopedKey.of(payloadKey), payload)
    }
}

internal actual object TraktSettingsStorage {
    private const val namespace = "nuvio_trakt_settings"
    private const val payloadKey = "trakt_settings_payload"

    actual fun loadPayload(): String? =
        WebKeyValueStorage.getString(namespace, ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        WebKeyValueStorage.setString(namespace, ProfileScopedKey.of(payloadKey), payload)
    }
}

internal actual object TraktCommentsStorage {
    private const val namespace = "nuvio_trakt_comments"
    private const val enabledKey = "trakt_comments_enabled"

    actual fun loadEnabled(): Boolean? =
        WebKeyValueStorage.getBoolean(namespace, ProfileScopedKey.of(enabledKey))

    actual fun saveEnabled(enabled: Boolean) {
        WebKeyValueStorage.setBoolean(namespace, ProfileScopedKey.of(enabledKey), enabled)
    }

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadEnabled()?.let { put(enabledKey, encodeSyncBoolean(it)) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        WebKeyValueStorage.remove(namespace, ProfileScopedKey.of(enabledKey))
        payload.decodeSyncBoolean(enabledKey)?.let(::saveEnabled)
    }
}

internal actual object TraktPlatformClock {
    actual fun nowEpochMs(): Long = webNowEpochMs()

    actual fun parseIsoDateTimeToEpochMs(value: String): Long? =
        webParseIsoDateTimeToEpochMs(value)

    actual fun availableProcessors(): Int = 1
}
