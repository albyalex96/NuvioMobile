package com.nuvio.app.features.library

internal actual object LibraryClock {
    actual fun nowEpochMs(): Long = com.nuvio.app.nowEpochMs()
}
