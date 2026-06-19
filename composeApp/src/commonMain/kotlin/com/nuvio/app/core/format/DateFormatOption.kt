package com.nuvio.app.core.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.i18n.localizedMonthName
import com.nuvio.app.core.i18n.localizedShortMonthName
import com.nuvio.app.features.settings.ThemeSettingsRepository

enum class DateFormatOption(val key: String) {
    YEAR_MONTH_DAY_TEXT("YEAR_MONTH_DAY_TEXT"),
    DAY_MONTH_YEAR_TEXT("DAY_MONTH_YEAR_TEXT"),
    MONTH_DAY_YEAR_TEXT("MONTH_DAY_YEAR_TEXT"),
    DAY_MONTH_YEAR_NUMERIC("DAY_MONTH_YEAR_NUMERIC"),
    MONTH_DAY_YEAR_NUMERIC("MONTH_DAY_YEAR_NUMERIC"),
    YEAR_MONTH_DAY_NUMERIC("YEAR_MONTH_DAY_NUMERIC"),
    DAY_SHORT_MONTH_YEAR("DAY_SHORT_MONTH_YEAR"),
    MONTH_DAY_YEAR_SHORT("MONTH_DAY_YEAR_SHORT"),
}

fun formatDateForDisplay(raw: String, format: DateFormatOption): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return raw
    val datePart = trimmed.substringBefore('T').trim()
    val parts = datePart.split('-')
    if (parts.size != 3) return raw
    val year = parts[0].toIntOrNull() ?: return raw
    val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return raw
    val day = parts[2].toIntOrNull()?.takeIf { it in 1..31 } ?: return raw
    return when (format) {
        DateFormatOption.YEAR_MONTH_DAY_TEXT -> "$year ${localizedMonthName(month)} $day"
        DateFormatOption.DAY_MONTH_YEAR_TEXT -> "$day ${localizedMonthName(month)} $year"
        DateFormatOption.MONTH_DAY_YEAR_TEXT -> "${localizedMonthName(month)} $day, $year"
        DateFormatOption.DAY_MONTH_YEAR_NUMERIC ->
            "${day.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}/$year"
        DateFormatOption.MONTH_DAY_YEAR_NUMERIC ->
            "${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')}/$year"
        DateFormatOption.YEAR_MONTH_DAY_NUMERIC ->
            "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
        DateFormatOption.DAY_SHORT_MONTH_YEAR ->
            "$day ${localizedShortMonthName(month)} $year"
        DateFormatOption.MONTH_DAY_YEAR_SHORT ->
            "${localizedShortMonthName(month)} $day, $year"
    }
}

fun formatDateWithoutYear(raw: String, format: DateFormatOption): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return raw
    val datePart = trimmed.substringBefore('T').trim()
    val parts = datePart.split('-')
    if (parts.size != 3) return raw
    val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return raw
    val day = parts[2].toIntOrNull()?.takeIf { it in 1..31 } ?: return raw
    return when (format) {
        DateFormatOption.DAY_MONTH_YEAR_TEXT, DateFormatOption.DAY_MONTH_YEAR_NUMERIC,
        DateFormatOption.DAY_SHORT_MONTH_YEAR -> "$day ${localizedMonthName(month)}"
        DateFormatOption.MONTH_DAY_YEAR_TEXT, DateFormatOption.MONTH_DAY_YEAR_NUMERIC,
        DateFormatOption.MONTH_DAY_YEAR_SHORT, DateFormatOption.YEAR_MONTH_DAY_TEXT,
        DateFormatOption.YEAR_MONTH_DAY_NUMERIC -> "${localizedMonthName(month)} $day"
    }
}

@Composable
fun rememberDateFormatOption(): DateFormatOption {
    val format by ThemeSettingsRepository.dateFormatOption.collectAsStateWithLifecycle()
    return format
}

fun DateFormatOption.formatPreview(): String =
    formatDateForDisplay("2025-02-01", this)
