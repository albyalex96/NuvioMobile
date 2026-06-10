package com.nuvio.app.features.player

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.nuvio.app.core.platform.commandQtNativePlayer
import com.nuvio.app.core.platform.commandQtNativePlayerString
import com.nuvio.app.core.platform.playQtNativeMedia
import com.nuvio.app.core.platform.takeQtNativePlayerSnapshot
import com.nuvio.app.core.platform.takeQtNativePlayerTracks
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

@Composable
actual fun PlatformPlayerSurface(
    sourceUrl: String,
    sourceAudioUrl: String?,
    streamType: String?,
    sourceHeaders: Map<String, String>,
    sourceResponseHeaders: Map<String, String>,
    externalSubtitles: List<com.nuvio.app.features.streams.StreamSubtitle>,
    useYoutubeChunkedPlayback: Boolean,
    modifier: Modifier,
    playWhenReady: Boolean,
    resizeMode: PlayerResizeMode,
    useNativeController: Boolean,
    onControllerReady: (PlayerEngineController) -> Unit,
    onSnapshot: (PlayerPlaybackSnapshot) -> Unit,
    onError: (String?) -> Unit,
) {
    val latestOnControllerReady = rememberUpdatedState(onControllerReady)
    val latestOnSnapshot = rememberUpdatedState(onSnapshot)
    val latestOnError = rememberUpdatedState(onError)
    val latestSourceUrl = rememberUpdatedState(sourceUrl)
    val latestSourceHeaders = rememberUpdatedState(sourceHeaders)
    val nativePlaybackAvailable = remember { mutableStateOf(false) }
    val nativeAudioTracks = remember { mutableStateOf<List<AudioTrack>>(emptyList()) }
    val nativeSubtitleTracks = remember { mutableStateOf<List<SubtitleTrack>>(emptyList()) }
    val controller = remember {
        object : PlayerEngineController {
            override fun play() {
                commandQtNativePlayer("resume", 0.0)
            }

            override fun pause() {
                commandQtNativePlayer("pause", 0.0)
            }

            override fun seekTo(positionMs: Long) {
                commandQtNativePlayer("seekTo", positionMs.toDouble())
            }

            override fun seekBy(offsetMs: Long) {
                commandQtNativePlayer("seekBy", offsetMs.toDouble())
            }

            override fun retry() {
                playQtNativeMedia(
                    latestSourceUrl.value,
                    latestSourceHeaders.value.toHeadersJson(),
                    0.0,
                )
            }

            override fun setPlaybackSpeed(speed: Float) {
                commandQtNativePlayer("setSpeed", speed.toDouble())
            }

            override fun getAudioTracks(): List<AudioTrack> =
                nativeAudioTracks.value

            override fun getSubtitleTracks(): List<SubtitleTrack> =
                nativeSubtitleTracks.value

            override fun selectAudioTrack(index: Int) {
                commandQtNativePlayer("selectAudioTrack", nativeAudioTracks.value.toNativeTrackIndex(index).toDouble())
            }

            override fun selectSubtitleTrack(index: Int) {
                commandQtNativePlayer("selectSubtitleTrack", nativeSubtitleTracks.value.toNativeTrackIndex(index).toDouble())
            }

            override fun setSubtitleUri(url: String) {
                commandQtNativePlayerString("setSubtitleUri", url)
            }

            override fun clearExternalSubtitle() {
                commandQtNativePlayer("clearExternalSubtitle", 0.0)
            }

            override fun clearExternalSubtitleAndSelect(trackIndex: Int) {
                commandQtNativePlayer(
                    "clearExternalSubtitleAndSelect",
                    nativeSubtitleTracks.value.toNativeTrackIndex(trackIndex).toDouble(),
                )
            }

            override fun applySubtitleStyle(style: SubtitleStyleState) {
                commandQtNativePlayerString("applySubtitleStyle", style.toQtNativeJson())
            }
        }
    }

    LaunchedEffect(sourceUrl, sourceAudioUrl, sourceHeaders) {
        latestOnControllerReady.value(controller)
        nativePlaybackAvailable.value = false
        val handedOff = playQtNativeMedia(
            sourceUrl,
            sourceHeaders.toHeadersJson(),
            0.0,
        )
        nativePlaybackAvailable.value = handedOff
        if (handedOff) {
            if (!playWhenReady) {
                commandQtNativePlayer("pause", 0.0)
            }
            latestOnSnapshot.value(
                PlayerPlaybackSnapshot(
                    isLoading = true,
                ),
            )
            latestOnError.value(null)
            nativeAudioTracks.value = emptyList()
            nativeSubtitleTracks.value = emptyList()
        } else {
            latestOnSnapshot.value(PlayerPlaybackSnapshot(isLoading = false))
            latestOnError.value("Playback is not available in this web runtime.")
        }
    }

    LaunchedEffect(playWhenReady) {
        if (!nativePlaybackAvailable.value) return@LaunchedEffect
        commandQtNativePlayer(if (playWhenReady) "resume" else "pause", 0.0)
    }

    LaunchedEffect(nativePlaybackAvailable.value, resizeMode) {
        if (!nativePlaybackAvailable.value) return@LaunchedEffect
        commandQtNativePlayer("setResizeMode", resizeMode.toQtNativeIndex().toDouble())
    }

    LaunchedEffect(nativePlaybackAvailable.value) {
        if (!nativePlaybackAvailable.value) return@LaunchedEffect
        var previousSnapshot = ""
        while (true) {
            val rawSnapshot = takeQtNativePlayerSnapshot()
            if (rawSnapshot.isNotBlank() && rawSnapshot != previousSnapshot) {
                rawSnapshot.toQtNativePlaybackSnapshotOrNull()?.let { snapshot ->
                    previousSnapshot = rawSnapshot
                    latestOnSnapshot.value(snapshot)
                }
            }
            delay(250)
        }
    }

    LaunchedEffect(nativePlaybackAvailable.value) {
        if (!nativePlaybackAvailable.value) return@LaunchedEffect
        var previousTracks = ""
        while (true) {
            val rawTracks = takeQtNativePlayerTracks()
            if (rawTracks.isNotBlank() && rawTracks != previousTracks) {
                rawTracks.toQtNativeTracksOrNull()?.let { tracks ->
                    previousTracks = rawTracks
                    nativeAudioTracks.value = tracks.audioTracks
                    nativeSubtitleTracks.value = tracks.subtitleTracks
                }
            }
            delay(500)
        }
    }

    DisposableEffect(sourceUrl) {
        onDispose {
            commandQtNativePlayer("stop", 0.0)
        }
    }

    Box(modifier = modifier)
}

private fun Map<String, String>.toHeadersJson(): String =
    entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
        "\"${key.jsonEscaped()}\":\"${value.jsonEscaped()}\""
    }

private fun String.jsonEscaped(): String = buildString(length + 8) {
    this@jsonEscaped.forEach { char ->
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(char)
        }
    }
}

private fun PlayerResizeMode.toQtNativeIndex(): Int =
    when (this) {
        PlayerResizeMode.Fit -> 0
        PlayerResizeMode.Fill -> 1
        PlayerResizeMode.Zoom -> 2
    }

private val qtNativeSnapshotJson = Json { ignoreUnknownKeys = true }

private fun String.toQtNativePlaybackSnapshotOrNull(): PlayerPlaybackSnapshot? =
    runCatching {
        val payload = qtNativeSnapshotJson.parseToJsonElement(this).jsonObject
        PlayerPlaybackSnapshot(
            isLoading = payload["loading"]?.jsonPrimitive?.booleanOrNull ?: false,
            isPlaying = payload["playing"]?.jsonPrimitive?.booleanOrNull ?: false,
            isEnded = payload["ended"]?.jsonPrimitive?.booleanOrNull ?: false,
            durationMs = payload["durationMs"]?.jsonPrimitive?.longOrNull ?: 0L,
            positionMs = payload["positionMs"]?.jsonPrimitive?.longOrNull ?: 0L,
            playbackSpeed = payload["playbackSpeed"]?.jsonPrimitive?.floatOrNull ?: 1f,
        )
    }.getOrNull()

private data class QtNativeTracks(
    val audioTracks: List<AudioTrack>,
    val subtitleTracks: List<SubtitleTrack>,
)

private fun String.toQtNativeTracksOrNull(): QtNativeTracks? =
    runCatching {
        val payload = qtNativeSnapshotJson.parseToJsonElement(this).jsonObject
        QtNativeTracks(
            audioTracks = payload["audio"]
                ?.jsonArray
                ?.mapNotNull { element ->
                    val row = element.jsonObject
                    val mpvIndex = row["mpvIndex"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
                    AudioTrack(
                        index = mpvIndex,
                        id = mpvIndex.toString(),
                        label = row["label"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                            ?: "Track $mpvIndex",
                        language = row["language"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
                        isSelected = row["selected"]?.jsonPrimitive?.booleanOrNull ?: false,
                    )
                }
                .orEmpty(),
            subtitleTracks = payload["subtitles"]
                ?.jsonArray
                ?.mapNotNull { element ->
                    val row = element.jsonObject
                    val mpvIndex = row["mpvIndex"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
                    val label = row["label"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                        ?: "Track $mpvIndex"
                    SubtitleTrack(
                        index = mpvIndex,
                        id = mpvIndex.toString(),
                        label = label,
                        language = row["language"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
                        isSelected = row["selected"]?.jsonPrimitive?.booleanOrNull ?: false,
                        isForced = label.contains("forced", ignoreCase = true),
                    )
                }
                .orEmpty(),
        )
    }.getOrNull()

private fun List<AudioTrack>.toNativeTrackIndex(index: Int): Int {
    if (index < 0) return -1
    return firstOrNull { it.index == index }?.id?.toIntOrNull()
        ?: getOrNull(index)?.id?.toIntOrNull()
        ?: index
}

private fun List<SubtitleTrack>.toNativeTrackIndex(index: Int): Int {
    if (index < 0) return -1
    return firstOrNull { it.index == index }?.id?.toIntOrNull()
        ?: getOrNull(index)?.id?.toIntOrNull()
        ?: index
}

private fun SubtitleStyleState.toQtNativeJson(): String = buildString {
    append('{')
    append("\"textColor\":\"").append(textColor.toQtNativeHexColor().jsonEscaped()).append("\",")
    append("\"outlineEnabled\":").append(outlineEnabled).append(',')
    append("\"fontSizeSp\":").append(fontSizeSp).append(',')
    append("\"bottomOffset\":").append(bottomOffset)
    append('}')
}

private fun androidx.compose.ui.graphics.Color.toQtNativeHexColor(): String {
    val argb = toArgb()
    val red = (argb shr 16) and 0xff
    val green = (argb shr 8) and 0xff
    val blue = argb and 0xff
    val alpha = (argb ushr 24) and 0xff
    return "#" + alpha.toHexByte() + red.toHexByte() + green.toHexByte() + blue.toHexByte()
}

private fun Int.toHexByte(): String =
    toString(16).uppercase().padStart(2, '0')
