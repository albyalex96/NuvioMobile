package com.nuvio.app.features.trakt

internal actual object TraktPlatformClock {
    actual fun nowEpochMs(): Long = com.nuvio.app.nowEpochMs()
    actual fun parseIsoDateTimeToEpochMs(value: String): Long? = com.nuvio.app.parseIsoDateTimeToEpochMs(value)
    actual fun availableProcessors(): Int = kotlinx.browser.window.navigator.hardwareConcurrency.toInt()
}
