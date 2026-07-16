package com.nuvio.app.features.watchprogress

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

actual object CurrentDateProvider {
    actual fun todayIsoDate(): String = LocalDate.now().toString()

    actual fun localStartOfDayEpochMs(isoDate: String): Long? {
        val date = runCatching { LocalDate.parse(isoDate) }.getOrNull() ?: return null
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
