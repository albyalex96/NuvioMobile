package com.nuvio.app.features.player.cast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nuvio.app.features.player.sanitizePlaybackHeaders
import kotlinx.serialization.json.Json

interface NuvioCastBridge {
    fun setListener(listener: CastBridgeListener?)

    fun startDiscovery()
    fun stopDiscovery()
    fun getDeviceCount(): Int
    fun getDeviceId(at: Int): String
    fun getDeviceName(at: Int): String
    fun getDeviceModel(at: Int): String

    fun connect(deviceId: String)
    fun disconnect()

    fun getConnectionState(): Int
    fun getConnectedDeviceName(): String

    fun loadMedia(
        url: String,
        title: String,
        subtitle: String,
        posterUrl: String,
        contentType: String,
        startPositionMs: Long,
        headersJson: String,
    )

    fun play()
    fun pause()
    fun seekTo(positionMs: Long)

    fun isCasting(): Boolean
    fun getPositionMs(): Long
    fun getDurationMs(): Long
    fun getIsPlaying(): Boolean
    fun getIsBuffering(): Boolean

    fun destroy()
}

interface CastBridgeListener {
    fun onCastStateChanged()
}

object NuvioCastBridgeFactory {
    private var factoryRef: NuvioCastBridgeCreator? = null

    fun registerFactory(creator: NuvioCastBridgeCreator) {
        this.factoryRef = creator
    }

    fun create(): NuvioCastBridge? = factoryRef?.createBridge()

    val isRegistered: Boolean get() = factoryRef != null
}

interface NuvioCastBridgeCreator {
    fun createBridge(): NuvioCastBridge
}

object NuvioDlnaBridgeFactory {
    private var factoryRef: NuvioCastBridgeCreator? = null

    fun registerFactory(creator: NuvioCastBridgeCreator) {
        this.factoryRef = creator
    }

    fun create(): NuvioCastBridge? = factoryRef?.createBridge()

    val isRegistered: Boolean get() = factoryRef != null
}

private fun encodeCastHeaders(headers: Map<String, String>): String {
    val sanitized = sanitizePlaybackHeaders(headers)
    if (sanitized.isEmpty()) return ""
    return runCatching { Json.encodeToString(sanitized) }.getOrDefault("")
}

private class IosCastController(private val bridge: NuvioCastBridge) : CastController {

    override var connectionState by mutableStateOf(mapState(bridge.getConnectionState()))
        private set

    private val deviceList = mutableStateListOf<CastDevice>()
    override val devices: List<CastDevice> get() = deviceList

    override var connectedDeviceName by mutableStateOf<String?>(null)
        private set

    override var isCasting by mutableStateOf(false)
        private set

    override var playbackSnapshot by mutableStateOf(CastPlaybackSnapshot())
        private set

    private val listener = object : CastBridgeListener {
        override fun onCastStateChanged() = sync()
    }

    fun attach() {
        bridge.setListener(listener)
        sync()
    }

    fun detach() {
        bridge.setListener(null)
    }

    override fun startDiscovery() {
        bridge.startDiscovery()
        refreshDevices()
    }

    override fun stopDiscovery() {
        bridge.stopDiscovery()
    }

    override fun connect(device: CastDevice) {
        bridge.connect(device.id)
    }

    override fun disconnect() {
        bridge.disconnect()
    }

    override fun loadMedia(request: CastMediaRequest) {
        bridge.loadMedia(
            url = request.url,
            title = request.title,
            subtitle = request.subtitle ?: "",
            posterUrl = request.posterUrl ?: "",
            contentType = request.contentType ?: guessCastContentType(request.url),
            startPositionMs = request.startPositionMs,
            headersJson = encodeCastHeaders(request.headers),
        )
    }

    override fun play() = bridge.play()
    override fun pause() = bridge.pause()
    override fun seekTo(positionMs: Long) = bridge.seekTo(positionMs)

    private fun sync() {
        connectionState = mapState(bridge.getConnectionState())
        connectedDeviceName = bridge.getConnectedDeviceName().ifBlank { null }
        isCasting = bridge.isCasting()
        playbackSnapshot = CastPlaybackSnapshot(
            isPlaying = bridge.getIsPlaying(),
            isBuffering = bridge.getIsBuffering(),
            positionMs = bridge.getPositionMs(),
            durationMs = bridge.getDurationMs(),
        )
        refreshDevices()
    }

    private fun refreshDevices() {
        deviceList.clear()
        val count = bridge.getDeviceCount()
        for (i in 0 until count) {
            deviceList.add(
                CastDevice(
                    id = bridge.getDeviceId(i),
                    name = bridge.getDeviceName(i),
                    modelName = bridge.getDeviceModel(i).ifBlank { null },
                ),
            )
        }
    }

    private companion object {
        fun mapState(raw: Int): CastConnectionState = when (raw) {
            0 -> CastConnectionState.Unavailable
            2 -> CastConnectionState.Connecting
            3 -> CastConnectionState.Connected
            else -> CastConnectionState.NotConnected
        }
    }
}

private const val DLNA_ID_PREFIX = "dlna::"

private class CombinedIosCastController(
    private val cast: IosCastController?,
    private val dlna: IosCastController?,
) : CastController {

    override val connectionState: CastConnectionState
        get() {
            val c = cast?.connectionState ?: CastConnectionState.Unavailable
            val d = dlna?.connectionState ?: CastConnectionState.Unavailable
            return when {
                c == CastConnectionState.Connected || d == CastConnectionState.Connected -> CastConnectionState.Connected
                c == CastConnectionState.Connecting || d == CastConnectionState.Connecting -> CastConnectionState.Connecting
                c == CastConnectionState.NotConnected || d == CastConnectionState.NotConnected -> CastConnectionState.NotConnected
                else -> CastConnectionState.Unavailable
            }
        }

    override val devices: List<CastDevice>
        get() = (cast?.devices ?: emptyList()) + (dlna?.devices ?: emptyList())

    override val connectedDeviceName: String?
        get() = cast?.connectedDeviceName ?: dlna?.connectedDeviceName

    override val isCasting: Boolean
        get() = cast?.isCasting == true || dlna?.isCasting == true

    override val playbackSnapshot: CastPlaybackSnapshot
        get() = when {
            dlna?.isCasting == true -> dlna.playbackSnapshot
            cast?.isCasting == true -> cast.playbackSnapshot
            else -> CastPlaybackSnapshot()
        }

    private val dlnaActive: Boolean
        get() = dlna != null &&
            (dlna.connectionState == CastConnectionState.Connected || dlna.isCasting)

    fun attach() {
        cast?.attach()
        dlna?.attach()
    }

    fun detach() {
        cast?.detach()
        dlna?.detach()
    }

    override fun startDiscovery() {
        cast?.startDiscovery()
        dlna?.startDiscovery()
    }

    override fun stopDiscovery() {
        cast?.stopDiscovery()
        dlna?.stopDiscovery()
    }

    override fun connect(device: CastDevice) {
        if (device.id.startsWith(DLNA_ID_PREFIX)) dlna?.connect(device) else cast?.connect(device)
    }

    override fun disconnect() {
        dlna?.disconnect()
        cast?.disconnect()
    }

    override fun loadMedia(request: CastMediaRequest) {
        if (dlnaActive) dlna?.loadMedia(request) else cast?.loadMedia(request)
    }

    override fun play() {
        if (dlnaActive) dlna?.play() else cast?.play()
    }

    override fun pause() {
        if (dlnaActive) dlna?.pause() else cast?.pause()
    }

    override fun seekTo(positionMs: Long) {
        if (dlnaActive) dlna?.seekTo(positionMs) else cast?.seekTo(positionMs)
    }
}

@Composable
actual fun rememberCastController(): CastController? {
    val controller = remember {
        val cast = NuvioCastBridgeFactory.create()?.let(::IosCastController)
        val dlna = NuvioDlnaBridgeFactory.create()?.let(::IosCastController)
        if (cast == null && dlna == null) null else CombinedIosCastController(cast, dlna)
    }
    if (controller != null) {
        DisposableEffect(controller) {
            controller.attach()
            onDispose { controller.detach() }
        }
    }
    return controller
}
