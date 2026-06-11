package com.nuvio.app.features.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.nuvio.app.core.platform.jsVideoCaptureFrame
import com.nuvio.app.core.platform.jsVideoCreate
import com.nuvio.app.core.platform.jsVideoDestroy
import com.nuvio.app.core.platform.jsVideoGetSnapshot
import com.nuvio.app.core.platform.jsVideoGetTrackInfo
import com.nuvio.app.core.platform.jsVideoPause
import com.nuvio.app.core.platform.jsVideoPlay
import com.nuvio.app.core.platform.jsVideoSeekBy
import com.nuvio.app.core.platform.jsVideoSeekTo
import com.nuvio.app.core.platform.jsVideoSelectAudioTrack
import com.nuvio.app.core.platform.jsVideoSetMuted
import com.nuvio.app.core.platform.jsVideoSetSpeed
import com.nuvio.app.core.platform.jsVideoSetupSource
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
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
import org.jetbrains.skia.Image as SkiaImage

private const val FRAME_CAPTURE_QUALITY = 0.55
private const val SNAPSHOT_POLL_MS = 250L
private const val FRAME_CAPTURE_INTERVAL_MS = 40L

@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
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
    val latestOnSnapshot = rememberUpdatedState(onSnapshot)
    val latestOnError = rememberUpdatedState(onError)
    val latestSourceUrl = rememberUpdatedState(sourceUrl)
    val latestSourceHeaders = rememberUpdatedState(sourceHeaders)
    val latestPlayWhenReady = rememberUpdatedState(playWhenReady)
    val currentFrame = remember { mutableStateOf<ImageBitmap?>(null) }
    val audioTracksState = remember { mutableStateOf<List<AudioTrack>>(emptyList()) }
    val sourceReady = remember { mutableStateOf(false) }

    val controller = remember {
        object : PlayerEngineController {
            override fun play() { if (sourceReady.value) jsVideoPlay() }
            override fun pause() { if (sourceReady.value) jsVideoPause() }
            override fun seekTo(positionMs: Long) { if (sourceReady.value) jsVideoSeekTo(positionMs.toDouble()) }
            override fun seekBy(offsetMs: Long) { if (sourceReady.value) jsVideoSeekBy(offsetMs.toDouble()) }
            override fun retry() {
                sourceReady.value = false
                val result = jsVideoSetupSource(
                    latestSourceUrl.value,
                    streamType,
                    latestSourceHeaders.value.toHeadersJson(),
                )
                if (parseSetupOk(result)) {
                    sourceReady.value = true
                }
                if (!latestPlayWhenReady.value) jsVideoPause()
            }
            override fun setPlaybackSpeed(speed: Float) { jsVideoSetSpeed(speed.toDouble()) }
            override fun setMuted(muted: Boolean) { jsVideoSetMuted(muted) }
            override fun getAudioTracks(): List<AudioTrack> = audioTracksState.value
            override fun getSubtitleTracks(): List<SubtitleTrack> = emptyList()
            override fun selectAudioTrack(index: Int) { jsVideoSelectAudioTrack(index) }
            override fun selectSubtitleTrack(index: Int) {}
            override fun setSubtitleUri(url: String) {}
            override fun clearExternalSubtitle() {}
            override fun clearExternalSubtitleAndSelect(trackIndex: Int) {}
            override fun applySubtitleStyle(style: SubtitleStyleState) {}
        }
    }

    LaunchedEffect(Unit) {
        jsVideoCreate()
        onControllerReady(controller)
    }

    LaunchedEffect(sourceUrl, sourceAudioUrl, streamType, sourceHeaders) {
        sourceReady.value = false
        currentFrame.value = null
        val jpg = sourceHeaders.toHeadersJson()
        val result = jsVideoSetupSource(sourceUrl, streamType, jpg)
        if (parseSetupOk(result)) {
            sourceReady.value = true
            latestOnError.value(null)
        } else {
            latestOnSnapshot.value(PlayerPlaybackSnapshot(isLoading = false))
            latestOnError.value("Failed to load source")
            return@LaunchedEffect
        }
        if (!playWhenReady) jsVideoPause()
    }

    LaunchedEffect(playWhenReady) {
        if (!sourceReady.value) return@LaunchedEffect
        if (playWhenReady) jsVideoPlay() else jsVideoPause()
    }

    LaunchedEffect(sourceReady.value) {
        if (!sourceReady.value) return@LaunchedEffect
        var previousSnapshot = ""
        while (true) {
            val raw = jsVideoGetSnapshot()
            if (raw.isNotBlank() && raw != previousSnapshot) {
                previousSnapshot = raw
                raw.toPlaybackSnapshotOrNull()?.let { snapshot ->
                    latestOnSnapshot.value(snapshot)
                }
            }
            delay(SNAPSHOT_POLL_MS)
        }
    }

    LaunchedEffect(sourceReady.value) {
        if (!sourceReady.value) return@LaunchedEffect
        while (true) {
            val raw = jsVideoGetTrackInfo()
            if (raw.isNotBlank()) {
                raw.toTrackInfoOrNull()?.let { audioTracksState.value = it }
            }
            delay(2000)
        }
    }

    LaunchedEffect(sourceReady.value) {
        if (!sourceReady.value) return@LaunchedEffect
        while (true) {
            val b64 = jsVideoCaptureFrame(FRAME_CAPTURE_QUALITY)
            if (b64.isNotBlank()) {
                try {
                    val bytes = Base64.decode(b64)
                    val skiaImage = SkiaImage.makeFromEncoded(bytes)
                    currentFrame.value = skiaImage.toComposeImageBitmap()
                } catch (_: Exception) { }
            }
            delay(FRAME_CAPTURE_INTERVAL_MS)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            currentFrame.value = null
            jsVideoDestroy()
        }
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val frame = currentFrame.value ?: return@Canvas
            val dstW = size.width.toInt()
            val dstH = size.height.toInt()
            if (dstW <= 0 || dstH <= 0) return@Canvas
            val srcW = frame.width
            val srcH = frame.height
            if (srcW <= 0 || srcH <= 0) return@Canvas

            when (resizeMode) {
                PlayerResizeMode.Fit -> {
                    val srcRatio = srcW.toFloat() / srcH.toFloat()
                    val dstRatio = dstW.toFloat() / dstH.toFloat()
                    val (drawW, drawH) = if (srcRatio > dstRatio) {
                        dstW.toFloat() to (dstW.toFloat() / srcRatio)
                    } else {
                        (dstH.toFloat() * srcRatio) to dstH.toFloat()
                    }
                    val ox = ((dstW - drawW) / 2f).toInt()
                    val oy = ((dstH - drawH) / 2f).toInt()
                    drawImage(frame, dstOffset = IntOffset(ox, oy), dstSize = IntSize(drawW.toInt(), drawH.toInt()))
                }
                PlayerResizeMode.Fill -> {
                    drawImage(frame, dstSize = IntSize(dstW, dstH))
                }
                PlayerResizeMode.Zoom -> {
                    val srcRatio = srcW.toFloat() / srcH.toFloat()
                    val dstRatio = dstW.toFloat() / dstH.toFloat()
                    val scale = if (srcRatio > dstRatio) dstH.toFloat() / srcH.toFloat() else dstW.toFloat() / srcW.toFloat()
                    val drawW = srcW * scale
                    val drawH = srcH * scale
                    val ox = ((dstW - drawW) / 2f).toInt()
                    val oy = ((dstH - drawH) / 2f).toInt()
                    drawImage(frame, dstOffset = IntOffset(ox, oy), dstSize = IntSize(drawW.toInt(), drawH.toInt()))
                }
            }
        }
    }
}

private fun parseSetupOk(json: String): Boolean =
    runCatching {
        val obj = Json.parseToJsonElement(json).jsonObject
        obj["ok"]?.jsonPrimitive?.booleanOrNull == true
    }.getOrDefault(false)

private val snapshotJson = Json { ignoreUnknownKeys = true }

private fun String.toPlaybackSnapshotOrNull(): PlayerPlaybackSnapshot? =
    runCatching {
        val p = snapshotJson.parseToJsonElement(this).jsonObject
        val ready = p["readyState"]?.jsonPrimitive?.intOrNull ?: 0
        val paused = p["paused"]?.jsonPrimitive?.booleanOrNull ?: true
        val ended = p["ended"]?.jsonPrimitive?.booleanOrNull ?: false
        val posMs = ((p["currentTime"]?.jsonPrimitive?.floatOrNull ?: 0f) * 1000f).toLong()
        val durMs = ((p["duration"]?.jsonPrimitive?.floatOrNull ?: 0f) * 1000f).toLong()
        val bufEndMs = ((p["bufferedEnd"]?.jsonPrimitive?.floatOrNull ?: 0f) * 1000f).toLong()
        val speed = p["playbackRate"]?.jsonPrimitive?.floatOrNull ?: 1f
        val seeking = p["seeking"]?.jsonPrimitive?.booleanOrNull ?: false
        val isLoading = (ready < 4 && !paused && !ended) || seeking || ready < 2
        val isPlaying = !paused && !ended && ready >= 2 && !seeking
        PlayerPlaybackSnapshot(
            isLoading = isLoading,
            isPlaying = isPlaying,
            isEnded = ended,
            durationMs = durMs,
            positionMs = posMs,
            bufferedPositionMs = bufEndMs,
            playbackSpeed = speed,
        )
    }.getOrNull()

private fun String.toTrackInfoOrNull(): List<AudioTrack>? =
    runCatching {
        val p = snapshotJson.parseToJsonElement(this).jsonObject
        p["audioTracks"]?.jsonArray?.mapNotNull { element ->
            val row = element.jsonObject
            val index = row["index"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            AudioTrack(
                index = index,
                id = row["id"]?.jsonPrimitive?.contentOrNull ?: index.toString(),
                label = row["label"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                    ?: "Track $index",
                language = row["language"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
                isSelected = row["enabled"]?.jsonPrimitive?.booleanOrNull ?: false,
            )
        }.orEmpty()
    }.getOrNull()

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
