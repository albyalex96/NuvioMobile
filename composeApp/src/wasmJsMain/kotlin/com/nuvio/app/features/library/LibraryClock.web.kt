package com.nuvio.app.features.library

import com.nuvio.app.core.platform.webNowEpochMs

internal actual object LibraryClock {
    actual fun nowEpochMs(): Long = webNowEpochMs()
}
