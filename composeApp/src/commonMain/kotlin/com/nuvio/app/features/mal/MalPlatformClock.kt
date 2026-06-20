package com.nuvio.app.features.mal

internal expect object MalPlatformClock {
    fun nowEpochMs(): Long
}
