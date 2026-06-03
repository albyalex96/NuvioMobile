package com.nuvio.app.features.watchprogress

internal actual object WatchProgressClock {
    actual fun nowEpochMs(): Long = com.nuvio.app.nowEpochMs()
}
