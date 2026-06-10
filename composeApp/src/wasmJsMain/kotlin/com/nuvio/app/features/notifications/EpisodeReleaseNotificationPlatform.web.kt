package com.nuvio.app.features.notifications

import com.nuvio.app.core.platform.WebKeyValueStorage
import com.nuvio.app.core.platform.webIsoDateFromEpochMs
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object EpisodeReleaseNotificationPlatform {
    actual suspend fun notificationsAuthorized(): Boolean = false

    actual suspend fun requestAuthorization(): Boolean = false

    actual suspend fun scheduleEpisodeReleaseNotifications(requests: List<EpisodeReleaseNotificationRequest>) = Unit

    actual suspend fun clearScheduledEpisodeReleaseNotifications() = Unit

    actual suspend fun showTestNotification(request: EpisodeReleaseNotificationRequest) = Unit
}

internal actual object EpisodeReleaseNotificationsClock {
    actual fun isoDateFromEpochMs(epochMs: Long): String = webIsoDateFromEpochMs(epochMs)
}

internal actual object EpisodeReleaseNotificationsStorage {
    private const val namespace = "nuvio_episode_release_notifications"
    private const val payloadKey = "episode_release_notifications_payload"

    actual fun loadPayload(): String? =
        WebKeyValueStorage.getString(namespace, ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        WebKeyValueStorage.setString(namespace, ProfileScopedKey.of(payloadKey), payload)
    }
}
