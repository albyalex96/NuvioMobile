package com.nuvio.app.features.watched

import com.nuvio.app.core.platform.webNowEpochMs

actual object WatchedClock {
    actual fun nowEpochMs(): Long = webNowEpochMs()
}
