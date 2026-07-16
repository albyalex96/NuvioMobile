package com.nuvio.app.features.trakt

import com.nuvio.app.core.storage.DesktopStorage

internal actual object TraktSettingsStorage {
    private val store = DesktopStorage.store("nuvio_trakt_settings")

    actual fun loadPayload(): String? = store.getString("trakt_settings_payload")

    actual fun savePayload(payload: String) = store.putString("trakt_settings_payload", payload)

    actual fun loadPendingWatchProgressSourcePayload(profileId: Int): String? =
        store.getString("pending_watch_progress_source_$profileId")

    actual fun savePendingWatchProgressSourcePayload(profileId: Int, payload: String) {
        store.putString("pending_watch_progress_source_$profileId", payload)
    }

    actual fun clearPendingWatchProgressSourcePayload(profileId: Int) {
        store.remove("pending_watch_progress_source_$profileId")
    }
}
