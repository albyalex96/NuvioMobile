package com.nuvio.app.features.watchprogress

import com.nuvio.app.core.platform.webNowEpochMs

internal actual object WatchProgressClock {
    actual fun nowEpochMs(): Long = webNowEpochMs()
}
