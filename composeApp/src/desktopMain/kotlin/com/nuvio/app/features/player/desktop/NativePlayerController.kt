package com.nuvio.app.features.player.desktop

import com.nuvio.app.features.player.AudioTrack
import com.nuvio.app.features.player.PlayerEngineController
import com.nuvio.app.features.player.PlayerPlaybackSnapshot
import com.nuvio.app.features.player.PlayerResizeMode
import com.nuvio.app.features.player.SUBTITLE_DELAY_MAX_MS
import com.nuvio.app.features.player.SUBTITLE_DELAY_MIN_MS
import com.nuvio.app.features.player.SubtitleStyleState
import com.nuvio.app.features.player.SubtitleTrack
import com.nuvio.app.features.player.inferForcedSubtitleTrack
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayInputStream
import javax.swing.SwingUtilities
import kotlin.concurrent.Volatile

internal class NativePlayerController(
    private val host: NativePlayerHost,
) : PlayerEngineController {
    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }

    @Volatile
    private var handle: Long = 0L
    private var pendingSource: PendingSource? = null
    private var onAction: (PlayerControlsAction) -> Boolean = { false }
    private var onEvent: (String, Double) -> Boolean = { _, _ -> false }
    private var onScrubChange: (Long) -> Boolean = { false }
    private var onScrubFinished: (Long) -> Boolean = { false }
    private val eventSink = NativePlayerEventSink { type, value ->
        SwingUtilities.invokeLater {
            handlePlayerEvent(type, value)
        }
    }

    fun setControlCallbacks(
        onAction: (PlayerControlsAction) -> Boolean,
        onEvent: (String, Double) -> Boolean,
        onScrubChange: (Long) -> Boolean,
        onScrubFinished: (Long) -> Boolean,
    ) {
        this.onAction = onAction
        this.onEvent = onEvent
        this.onScrubChange = onScrubChange
        this.onScrubFinished = onScrubFinished
    }

    fun attach(
        sourceUrl: String,
        sourceHeaders: Map<String, String>,
        playWhenReady: Boolean,
        initialPositionMs: Long,
        onError: (String?) -> Unit,
    ) {
        val pending = PendingSource(
            sourceUrl = sourceUrl,
            headerLines = sourceHeaders.toHeaderLines(),
            playWhenReady = playWhenReady,
            initialPositionMs = initialPositionMs.coerceAtLeast(0L),
            onError = onError,
        )
        pendingSource = pending
        host.onPeerReady = { attachPending() }
        if (host.isDisplayable) {
            attachPending()
        }
    }

    private fun attachPending() {
        val pending = pendingSource ?: return
        SwingUtilities.invokeLater {
            if (!host.isDisplayable) return@invokeLater
            disposePlayerHandle()
            runCatching {
                val hostViewPtr = AwtNativeViewResolver.resolveNativeViewPointer(host)
                handle = NativePlayerBridge.create(
                    hostViewPtr = hostViewPtr,
                    sourceUrl = pending.sourceUrl,
                    headerLines = pending.headerLines.toTypedArray(),
                    playWhenReady = pending.playWhenReady,
                    initialPositionMs = pending.initialPositionMs,
                    controlsPageUrl = NativePlayerBridge.controlsPageUrl,
                    eventSink = eventSink,
                )
                if (handle == 0L) error("Native player did not return a handle.")
                println("[NuvioDesktopPlayer] native player created, handle=$handle")
                if (pending.playWhenReady) {
                    NativePlayerBridge.setPaused(handle, false)
                    println("[NuvioDesktopPlayer] explicit play() after attach")
                }
            }.onFailure { error ->
                println("[NuvioDesktopPlayer] ERROR creating native player: ${error.message}")
                pending.onError(error.message)
            }
        }
    }

    fun setResizeMode(mode: PlayerResizeMode) {
        handle.takeIf { it != 0L }?.let { current ->
            NativePlayerBridge.setResizeMode(
                handle = current,
                mode = when (mode) {
                    PlayerResizeMode.Fit -> 0
                    PlayerResizeMode.Fill -> 1
                    PlayerResizeMode.Zoom -> 2
                },
            )
        }
    }

    private var subtitleDelayMs: Int = 0

    override fun updateControlsJson(json: String) {
        handle.takeIf { it != 0L }?.let { NativePlayerBridge.updateControls(it, json) }
    }

    fun errorMessage(): String {
        val current = handle
        if (current == 0L) return ""
        return runCatching { NativePlayerBridge.errorMessage(current) }.getOrDefault("")
    }

    private fun handlePlayerEvent(type: String, value: Double) {
        val skipIntervalSec = 10L
        when (type) {
            "cursorActivity" -> host.noteCursorActivity()
            "scrubChange" -> if (!onScrubChange(value.toLong())) { }
            "scrubFinish" -> {
                if (!onScrubFinished(value.toLong())) {
                    seekTo(value.toLong())
                }
            }
            "toggle", "keyboardToggle" -> togglePlayback()
            "back" -> onEvent("back", 0.0)
            "seekBack", "keyboardSeekBack" -> seekBy(-skipIntervalSec * 1000L)
            "seekForward", "keyboardSeekForward" -> seekBy(skipIntervalSec * 1000L)
            "toggleFullscreen" -> onEvent("toggleFullscreen", 0.0)
            "resize" -> cycleResizeMode()
            "speed" -> cyclePlaybackSpeed()
            "selectBuiltInSubtitleTrack" -> selectSubtitleTrack(value.toInt())
            "subtitles" -> onEvent("subtitles", 0.0)
            "audio" -> onEvent("audio", 0.0)
            "sources" -> onEvent("sources", 0.0)
            "episodes" -> onEvent("episodes", 0.0)
            "lock" -> onEvent("lock", 0.0)
            "keyboardVolumeUp" -> adjustVolume(5.0)
            "keyboardVolumeDown" -> adjustVolume(-5.0)
            "skipInterval" -> seekBy(value.toLong() * 1000L)
            "playNextEpisode" -> onEvent("playNextEpisode", 0.0)
            "subtitleDelayDelta" -> {
                subtitleDelayMs = (subtitleDelayMs + value.toInt()).coerceIn(SUBTITLE_DELAY_MIN_MS, SUBTITLE_DELAY_MAX_MS)
                handle.takeIf { it != 0L }?.let { NativePlayerBridge.setSubtitleDelayMs(it, subtitleDelayMs) }
            }
            "subtitleFontSizeDelta" -> onEvent("subtitleFontSizeDelta", value)
            "submitIntroCommit" -> onEvent("submitIntroCommit", value)
            "enableP2pForPlayerControls" -> onEvent("enableP2pForPlayerControls", value)
            "playbackError" -> {
                val err = errorMessage()
                if (err.isNotBlank()) onEvent("error", 0.0)
            }
            else -> onEvent(type, value)
        }
    }

    private fun togglePlayback() {
        val snapshot = snapshot()
        if (snapshot.isPlaying) pause() else play()
    }

    private var currentSpeedIndex = 0
    private val speedOptions = floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    private fun cyclePlaybackSpeed() {
        currentSpeedIndex = (currentSpeedIndex + 1) % speedOptions.size
        setPlaybackSpeed(speedOptions[currentSpeedIndex])
    }

    private var currentResizeMode = 0

    private fun cycleResizeMode() {
        currentResizeMode = (currentResizeMode + 1) % 3
        setResizeMode(
            when (currentResizeMode) {
                0 -> PlayerResizeMode.Fit
                1 -> PlayerResizeMode.Fill
                else -> PlayerResizeMode.Zoom
            }
        )
    }

    private fun adjustVolume(delta: Double) {
        val current = handle.takeIf { it != 0L } ?: return
        NativePlayerBridge.adjustVolume(current, delta.toFloat())
    }

    fun snapshot(): PlayerPlaybackSnapshot {
        val current = handle
        if (current == 0L) return PlayerPlaybackSnapshot(isLoading = true)
        return runCatching {
            val isLoading = NativePlayerBridge.isLoading(current)
            val isEnded = NativePlayerBridge.isEnded(current)
            PlayerPlaybackSnapshot(
                isLoading = isLoading,
                isPlaying = !NativePlayerBridge.isPaused(current) && !isLoading && !isEnded,
                isEnded = isEnded,
                durationMs = NativePlayerBridge.durationMs(current),
                positionMs = NativePlayerBridge.positionMs(current),
                bufferedPositionMs = NativePlayerBridge.bufferedPositionMs(current),
                playbackSpeed = NativePlayerBridge.speed(current),
            )
        }.getOrDefault(PlayerPlaybackSnapshot(isLoading = true))
    }

    fun dispose() {
        host.resetCursorVisibility()
        disposePlayerHandle()
    }

    private fun disposePlayerHandle() {
        val current = handle
        handle = 0L
        if (current != 0L) {
            runCatching { NativePlayerBridge.dispose(current) }
        }
    }

    override fun play() {
        handle.takeIf { it != 0L }?.let { NativePlayerBridge.setPaused(it, false) }
    }

    override fun pause() {
        handle.takeIf { it != 0L }?.let { NativePlayerBridge.setPaused(it, true) }
    }

    override fun seekTo(positionMs: Long) {
        handle.takeIf { it != 0L }?.let { NativePlayerBridge.seekTo(it, positionMs) }
    }

    override fun seekBy(offsetMs: Long) {
        handle.takeIf { it != 0L }?.let { NativePlayerBridge.seekBy(it, offsetMs) }
    }

    override fun retry() {
        val pending = pendingSource ?: return
        attach(
            sourceUrl = pending.sourceUrl,
            sourceHeaders = pending.headerLines.toHeaderMap(),
            playWhenReady = pending.playWhenReady,
            initialPositionMs = pending.initialPositionMs,
            onError = pending.onError,
        )
    }

    override fun setPlaybackSpeed(speed: Float) {
        handle.takeIf { it != 0L }?.let { NativePlayerBridge.setSpeed(it, speed) }
    }

    override fun setMuted(muted: Boolean) {}

    override fun getAudioTracks(): List<AudioTrack> =
        decodeTracks { NativePlayerBridge.audioTracksJson(it) }.map { track ->
            AudioTrack(
                index = track.index,
                id = track.id,
                label = track.label,
                language = track.language.takeUnless(String::isBlank),
                isSelected = track.selected,
            )
        }

    override fun getSubtitleTracks(): List<SubtitleTrack> =
        decodeTracks { NativePlayerBridge.subtitleTracksJson(it) }.map { track ->
            SubtitleTrack(
                index = track.index,
                id = track.id,
                label = track.label,
                language = track.language.takeUnless(String::isBlank),
                isSelected = track.selected,
                isForced = track.forced || inferForcedSubtitleTrack(
                    label = track.label,
                    language = track.language,
                    trackId = track.id,
                ),
            )
        }

    override fun selectAudioTrack(index: Int) {
        val current = handle.takeIf { it != 0L } ?: return
        val trackId = resolveTrackId(index, decodeTracks { NativePlayerBridge.audioTracksJson(it) }) ?: return
        NativePlayerBridge.selectAudioTrack(current, trackId)
    }

    override fun selectSubtitleTrack(index: Int) {
        val current = handle.takeIf { it != 0L } ?: return
        if (index < 0) {
            NativePlayerBridge.selectSubtitleTrack(current, -1)
            return
        }
        val trackId = resolveTrackId(index, decodeTracks { NativePlayerBridge.subtitleTracksJson(it) }) ?: return
        NativePlayerBridge.selectSubtitleTrack(current, trackId)
    }

    override fun setSubtitleUri(url: String) {
        handle.takeIf { it != 0L }?.let { NativePlayerBridge.addSubtitleUrl(it, url) }
    }

    override fun clearExternalSubtitle() {
        handle.takeIf { it != 0L }?.let(NativePlayerBridge::clearExternalSubtitles)
    }

    override fun clearExternalSubtitleAndSelect(trackIndex: Int) {
        val current = handle.takeIf { it != 0L } ?: return
        val trackId = if (trackIndex < 0) -1
        else resolveTrackId(trackIndex, decodeTracks { NativePlayerBridge.subtitleTracksJson(it) }) ?: return
        NativePlayerBridge.clearExternalSubtitlesAndSelect(current, trackId)
    }

    override fun setSubtitleDelayMs(delayMs: Int) {
        handle.takeIf { it != 0L }?.let { current ->
            NativePlayerBridge.setSubtitleDelayMs(
                current,
                delayMs.coerceIn(SUBTITLE_DELAY_MIN_MS, SUBTITLE_DELAY_MAX_MS),
            )
        }
    }

    override fun applySubtitleStyle(style: SubtitleStyleState) {
        handle.takeIf { it != 0L }?.let { current ->
            NativePlayerBridge.applySubtitleStyle(
                handle = current,
                textColor = style.textColor.toMpvColorString(),
                backgroundColor = style.backgroundColor.toMpvColorString(),
                outlineColor = style.outlineColor.toMpvColorString(),
                outlineSize = if (style.outlineEnabled) style.outlineWidth.toFloat() else 0f,
                bold = style.bold,
                fontSize = style.fontSizeSp.toFloat(),
                subPos = style.bottomOffset,
            )
        }
    }

    override fun configureIosVideoOutput(settings: com.nuvio.app.features.player.PlayerSettingsUiState) {}

    override fun setVolumeBoost(boostDb: Float) {
        handle.takeIf { it != 0L }?.let { NativePlayerBridge.setVolumeBoost(it, boostDb) }
    }

    private fun decodeTracks(readJson: (Long) -> String): List<NativeMpvTrack> {
        val current = handle.takeIf { it != 0L } ?: return emptyList()
        return runCatching {
            val jsonStr = readJson(current)
            json.decodeFromString<List<NativeMpvTrack>>(jsonStr)
        }.getOrDefault(emptyList())
    }
}

private data class PendingSource(
    val sourceUrl: String,
    val headerLines: List<String>,
    val playWhenReady: Boolean,
    val initialPositionMs: Long,
    val onError: (String?) -> Unit,
)

enum class PlayerControlsAction {
    ToggleChrome,
    RevealLockedOverlay,
    Back,
    TogglePlayback,
    KeyboardTogglePlayback,
    SeekBack,
    KeyboardSeekBack,
    SeekForward,
    KeyboardSeekForward,
    KeyboardVolumeDown,
    KeyboardVolumeUp,
    ResizeMode,
    Speed,
    Subtitles,
    Audio,
    Sources,
    Episodes,
    OpenExternalPlayer,
    SubmitIntro,
    LockToggle,
    VideoSettings,
    DoubleTapSeekBack,
    DoubleTapSeekForward,
}

@Serializable
private data class NativeMpvTrack(
    val index: Int = 0,
    val id: String = "",
    val label: String = "",
    val language: String = "",
    val selected: Boolean = false,
    val forced: Boolean = false,
)

private fun resolveTrackId(index: Int, tracks: List<NativeMpvTrack>): Int? =
    tracks.firstNotNullOfOrNull { track ->
        if (track.index == index) track.id.toIntOrNull()
        else null
    } ?: tracks.getOrNull(index)?.id?.toIntOrNull()

internal fun Map<String, String>.toHeaderLines(): List<String> =
    map { (key, value) -> "$key: $value" }

internal fun List<String>.toHeaderMap(): Map<String, String> =
    mapNotNull { line ->
        val colon = line.indexOf(": ")
        if (colon < 0) null
        else line.substring(0, colon) to line.substring(colon + 2)
    }.toMap()

private fun androidx.compose.ui.graphics.Color.toMpvColorString(): String {
    val alphaInt = (alpha * 255f).toInt().coerceIn(0, 255)
    val redInt = (red * 255f).toInt().coerceIn(0, 255)
    val greenInt = (green * 255f).toInt().coerceIn(0, 255)
    val blueInt = (blue * 255f).toInt().coerceIn(0, 255)
    return buildString {
        append('#')
        append(alphaInt.toHexByte())
        append(redInt.toHexByte())
        append(greenInt.toHexByte())
        append(blueInt.toHexByte())
    }
}

private fun Int.toHexByte(): String = toString(16).padStart(2, '0').uppercase()
