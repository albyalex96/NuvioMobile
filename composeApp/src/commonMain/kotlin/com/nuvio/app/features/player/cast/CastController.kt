package com.nuvio.app.features.player.cast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
interface CastController {
    val connectionState: CastConnectionState
    val devices: List<CastDevice>
    val connectedDeviceName: String?
    val isCasting: Boolean
    val playbackSnapshot: CastPlaybackSnapshot

    fun startDiscovery()
    fun stopDiscovery()
    fun connect(device: CastDevice)
    fun disconnect()
    fun loadMedia(request: CastMediaRequest)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
}

@Composable
expect fun rememberCastController(): CastController?
