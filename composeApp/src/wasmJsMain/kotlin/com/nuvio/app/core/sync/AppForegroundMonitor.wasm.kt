package com.nuvio.app.core.sync

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

internal actual object AppForegroundMonitor {
    private val _events = Channel<Unit>(Channel.CONFLATED)

    actual fun events(): Flow<Unit> = _events.receiveAsFlow()
}
