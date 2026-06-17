package com.nuvio.app.features.notifications

import com.nuvio.app.core.storage.DesktopStorage

internal actual object EpisodeReleaseNotificationsStorage {
    private val store = DesktopStorage.store("nuvio_episode_release_notifications")

    actual fun loadPayload(): String? =
        store.getString("episode_release_payload")

    actual fun savePayload(payload: String) {
        store.putString("episode_release_payload", payload)
    }
}
