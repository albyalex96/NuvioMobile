package com.nuvio.app.features.player

import java.util.Calendar

internal actual fun currentHour(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
