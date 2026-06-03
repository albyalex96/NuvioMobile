package com.nuvio.app.features.notifications

import com.nuvio.app.WebStorage

internal actual object EpisodeReleaseNotificationsStorage {
    private const val KEY = "nuvio_episode_release_notifications"

    actual fun loadPayload(): String? = WebStorage.getString(KEY)
    actual fun savePayload(payload: String) { WebStorage.setString(KEY, payload) }
}
