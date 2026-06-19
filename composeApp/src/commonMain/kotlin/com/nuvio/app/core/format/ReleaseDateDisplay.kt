package com.nuvio.app.core.format

fun formatReleaseDateForDisplay(raw: String): String =
    formatDateForDisplay(raw, DateFormatOption.YEAR_MONTH_DAY_TEXT)

fun formatReleaseDateWithoutYear(raw: String): String =
    formatDateWithoutYear(raw, DateFormatOption.YEAR_MONTH_DAY_TEXT)

fun extractReleaseYearForDisplay(raw: String): Int? {
    val t = raw.trim()
    if (t.isEmpty()) return null
    if (t.length == 4 && t.all { it.isDigit() }) {
        return t.toIntOrNull()?.takeIf { it in 1000..9999 }
    }
    val datePart = t.substringBefore('T').trim()
    val yearStr = datePart.split('-').firstOrNull() ?: return null
    return yearStr.toIntOrNull()?.takeIf { it in 1000..9999 }
}
