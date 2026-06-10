package com.nuvio.app.features.p2p

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual object P2pStreamingEngine {
    private val _state = MutableStateFlow<P2pStreamingState>(
        P2pStreamingState.Error("P2P streaming is not available on this platform.")
    )

    actual val state: StateFlow<P2pStreamingState> = _state.asStateFlow()

    actual fun warmup() = Unit

    actual fun cooldownWarmup() = Unit

    actual suspend fun startStream(request: P2pStreamRequest): String =
        throw P2pStreamingException("P2P streaming is not available on this platform.")

    actual fun stopStream() = Unit

    actual fun shutdown() = Unit
}
