package com.nuvio.app.features.mal

internal actual object MalPlatformClock {
    actual fun nowEpochMs(): Long = System.currentTimeMillis()
}
