package com.nuvio.app.features.player.skip

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal actual fun currentDateComponents(): DateComponents {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return DateComponents(year = now.year, month = now.monthNumber, day = now.dayOfMonth)
}
