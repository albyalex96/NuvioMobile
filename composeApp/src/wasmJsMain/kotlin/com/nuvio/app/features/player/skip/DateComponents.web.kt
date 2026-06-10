package com.nuvio.app.features.player.skip

import com.nuvio.app.core.platform.webTodayIsoDate

internal actual fun currentDateComponents(): DateComponents {
    val parts = webTodayIsoDate().split("-")
    return DateComponents(
        year = parts.getOrNull(0)?.toIntOrNull() ?: 1970,
        month = parts.getOrNull(1)?.toIntOrNull() ?: 1,
        day = parts.getOrNull(2)?.toIntOrNull() ?: 1,
    )
}
