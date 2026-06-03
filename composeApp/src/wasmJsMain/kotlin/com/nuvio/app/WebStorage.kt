package com.nuvio.app

import kotlinx.browser.localStorage
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal object WebStorage {
    fun getString(key: String): String? = localStorage.getItem(key)
    fun setString(key: String, value: String) { localStorage.setItem(key, value) }
    fun remove(key: String) { localStorage.removeItem(key) }
    fun getBoolean(key: String): Boolean? = getString(key)?.toBooleanStrictOrNull()
    fun setBoolean(key: String, value: Boolean) { setString(key, value.toString()) }
    fun getInt(key: String): Int? = getString(key)?.toIntOrNull()
    fun getFloat(key: String): Float? = getString(key)?.toFloatOrNull()
    fun getObject(key: String): String? = getString(key)
    fun setObject(key: String, value: String?) {
        if (value != null) setString(key, value) else remove(key)
    }
}

internal fun nowEpochMs(): Long = Clock.System.now().toEpochMilliseconds()

internal fun todayIsoDate(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val m = now.monthNumber.toString().padStart(2, '0')
    val d = now.dayOfMonth.toString().padStart(2, '0')
    return "${now.year}-$m-$d"
}

internal fun isoDateFromEpochMs(epochMs: Long): String {
    val instant = Instant.fromEpochMilliseconds(epochMs)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val m = local.monthNumber.toString().padStart(2, '0')
    val d = local.dayOfMonth.toString().padStart(2, '0')
    return "${local.year}-$m-$d"
}

internal fun parseIsoDateTimeToEpochMs(value: String): Long? {
    return try {
        Instant.parse(value).toEpochMilliseconds()
    } catch (_: Exception) {
        try {
            // Try parsing as date-only: YYYY-MM-DD
            val dateOnly = value.take(10)
            if (dateOnly.length != 10 || dateOnly[4] != '-' || dateOnly[7] != '-') return null
            Instant.parse("${dateOnly}T00:00:00Z").toEpochMilliseconds()
        } catch (_: Exception) { null }
    }
}

internal fun currentHour(): Int {
    return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
}
