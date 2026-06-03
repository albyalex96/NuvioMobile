package com.nuvio.app.features.notifications

internal actual object EpisodeReleaseNotificationsClock {
    actual fun isoDateFromEpochMs(epochMs: Long): String = com.nuvio.app.isoDateFromEpochMs(epochMs)
}
