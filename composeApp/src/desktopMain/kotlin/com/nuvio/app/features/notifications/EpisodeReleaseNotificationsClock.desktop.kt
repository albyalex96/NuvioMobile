package com.nuvio.app.features.notifications

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal actual object EpisodeReleaseNotificationsClock {
    actual fun isoDateFromEpochMs(epochMs: Long): String {
        return Instant.ofEpochMilli(epochMs)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()
    }
}
