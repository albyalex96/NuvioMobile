package com.nuvio.app.features.player

import kotlin.JsFun

@JsFun("() => new Date().getHours()")
private external fun jsCurrentHour(): Int

internal actual fun currentHour(): Int = jsCurrentHour()
