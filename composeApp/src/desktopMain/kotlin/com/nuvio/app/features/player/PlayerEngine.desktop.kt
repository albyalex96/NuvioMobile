package com.nuvio.app.features.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.logging.InAppLogger
import com.nuvio.app.features.player.desktop.DesktopHostOs
import com.nuvio.app.features.player.desktop.DesktopPlayerLaunchShield
import com.nuvio.app.features.player.desktop.NativePlayerController
import com.nuvio.app.features.player.desktop.NativePlayerHost
import kotlinx.coroutines.delay

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
    initialPositionMs: Long,
    useNativeController: Boolean,
    onControllerReady: (PlayerEngineController) -> Unit,
    onSnapshot: (PlayerPlaybackSnapshot) -> Unit,
    onError: (String?) -> Unit,
    onOverlayEvent: ((type: String, value: Double) -> Unit)?,
) {
    if (DesktopHostOs.current == DesktopHostOs.MACOS || DesktopHostOs.current == DesktopHostOs.WINDOWS) {
        NativePlayerSurface(
            sourceUrl = sourceUrl,
            sourceHeaders = sourceHeaders,
            externalSubtitles = externalSubtitles,
            modifier = modifier,
            playWhenReady = playWhenReady,
            resizeMode = resizeMode,
            initialPositionMs = initialPositionMs,
            onControllerReady = onControllerReady,
            onSnapshot = onSnapshot,
            onError = onError,
            onOverlayEvent = onOverlayEvent,
        )
        return
    }

    Box(modifier = modifier)
}

@Composable
private fun NativePlayerSurface(
    sourceUrl: String,
    sourceHeaders: Map<String, String>,
    externalSubtitles: List<com.nuvio.app.features.streams.StreamSubtitle>,
    modifier: Modifier,
    playWhenReady: Boolean,
    resizeMode: PlayerResizeMode,
    initialPositionMs: Long = 0L,
    onControllerReady: (PlayerEngineController) -> Unit,
    onSnapshot: (PlayerPlaybackSnapshot) -> Unit,
    onError: (String?) -> Unit,
    onOverlayEvent: ((type: String, value: Double) -> Unit)?,
) {
    val host = remember { NativePlayerHost() }
    val controller = remember(host) { NativePlayerController(host) }
    val hostFirstPaintComplete = remember { mutableStateOf(false) }
    val hostFirstFullSizePaintComplete = remember { mutableStateOf(false) }
    val latestOnOverlayEvent = rememberUpdatedState(onOverlayEvent)

    LaunchedEffect(sourceUrl) {
        DesktopPlayerLaunchShield.showForActiveWindow()
    }

    val playbackHeaders = remember(sourceHeaders) { sanitizePlaybackHeaders(sourceHeaders) }
    val latestOnError = rememberUpdatedState(onError)

    LaunchedEffect(controller) {
        controller.setControlCallbacks(
            onAction = { false },
            onEvent = { type, value ->
                latestOnOverlayEvent.value?.invoke(type, value)
                false
            },
            onScrubChange = { false },
            onScrubFinished = { false },
        )
    }

    LaunchedEffect(controller) {
        onControllerReady(controller)
    }

    DisposableEffect(host) {
        host.onDisplayableChanged = { displayable ->
            if (!displayable) {
                hostFirstPaintComplete.value = false
                hostFirstFullSizePaintComplete.value = false
            }
        }
        host.onFirstPaint = {
            hostFirstPaintComplete.value = true
        }
        host.onFirstFullSizePaint = {
            hostFirstFullSizePaintComplete.value = true
            DesktopPlayerLaunchShield.hideAfter()
        }
        onDispose {
            host.onDisplayableChanged = null
            host.onFirstPaint = null
            host.onFirstFullSizePaint = null
            DesktopPlayerLaunchShield.hide()
        }
    }

    DisposableEffect(controller, sourceUrl, playbackHeaders) {
        onDispose { controller.dispose() }
    }

    LaunchedEffect(controller, sourceUrl, playbackHeaders, hostFirstFullSizePaintComplete.value) {
        if (!hostFirstFullSizePaintComplete.value) return@LaunchedEffect
        delay(16L)
        controller.attach(
            sourceUrl = sourceUrl,
            sourceHeaders = playbackHeaders,
            playWhenReady = playWhenReady,
            initialPositionMs = initialPositionMs,
            onError = { message -> latestOnError.value(message) },
        )
    }

    LaunchedEffect(controller, playWhenReady) {
        if (playWhenReady) controller.play()
        else controller.pause()
    }

    LaunchedEffect(controller, resizeMode) {
        controller.setResizeMode(resizeMode)
    }

    LaunchedEffect(controller) {
        var lastError = ""
        val latestOnErrorVal = latestOnError
        while (true) {
            val snapshot = controller.snapshot()
            onSnapshot(snapshot)
            val err = controller.errorMessage()
            if (err.isNotBlank() && err != lastError) {
                lastError = err
                println("[NuvioDesktopPlayer] mpv error: $err")
                InAppLogger.error("Player/Desktop", "mpv error: $err")
                latestOnErrorVal.value(err)
            }
            if (!snapshot.isLoading && !snapshot.isPlaying && !snapshot.isEnded && snapshot.durationMs > 0) {
                println("[NuvioDesktopPlayer] stalled: pos=${snapshot.positionMs} dur=${snapshot.durationMs} playing=${snapshot.isPlaying} ended=${snapshot.isEnded} err=${err}")
                InAppLogger.warn("Player/Desktop", "stalled: pos=${snapshot.positionMs} dur=${snapshot.durationMs} playing=${snapshot.isPlaying} ended=${snapshot.isEnded} err=${err}")
            }
            delay(500L)
        }
    }

    LaunchedEffect(controller, externalSubtitles) {
        if (externalSubtitles.isEmpty()) return@LaunchedEffect
        var pollCount = 0
        while (pollCount < 40) {
            val snap = controller.snapshot()
            if (snap.durationMs > 0L || !snap.isLoading) break
            delay(250L)
            pollCount++
        }
        delay(500L)
        externalSubtitles.forEach { sub ->
            println("[NuvioDesktopPlayer] loading external subtitle: ${sub.url}")
            InAppLogger.info("Player/Desktop", "loading external subtitle: ${sub.url}")
            controller.setSubtitleUri(sub.url)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        SwingPanel(
            factory = { host },
            modifier = if (hostFirstPaintComplete.value) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .align(Alignment.BottomEnd)
                    .requiredSize(1.dp)
            },
            background = Color.Black,
        )
    }
}
