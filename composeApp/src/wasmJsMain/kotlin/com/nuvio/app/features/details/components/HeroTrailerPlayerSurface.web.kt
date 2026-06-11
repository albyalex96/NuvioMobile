package com.nuvio.app.features.details.components

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
import com.nuvio.app.core.platform.jsVideoPause
import com.nuvio.app.core.platform.jsVideoPlay
import com.nuvio.app.core.platform.jsVideoSetMuted
import com.nuvio.app.core.platform.jsVideoSetupSource
import kotlinx.coroutines.delay
import org.jetbrains.skia.Image as SkiaImage

@Composable
actual fun HeroTrailerPlayerSurface(
    sourceUrl: String,
    sourceAudioUrl: String?,
    playWhenReady: Boolean,
    muted: Boolean,
    modifier: Modifier,
    onReady: () -> Unit,
    onEnded: () -> Unit,
    onError: () -> Unit,
) {
    val latestOnReady = rememberUpdatedState(onReady)
    val latestOnEnded = rememberUpdatedState(onEnded)
    val latestOnError = rememberUpdatedState(onError)
    val currentFrame = remember { mutableStateOf<ImageBitmap?>(null) }
    val hasEnded = remember { mutableStateOf(false) }
    val isReady = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { jsVideoCreate() }

    LaunchedEffect(sourceUrl) {
        hasEnded.value = false
        isReady.value = false
        currentFrame.value = null
        if (sourceUrl.isBlank()) return@LaunchedEffect
        jsVideoSetupSource(sourceUrl, null, "{}")
        jsVideoSetMuted(muted)
        jsVideoPlay()
        isReady.value = true
        latestOnReady.value()
    }

    LaunchedEffect(muted) { jsVideoSetMuted(muted) }

    LaunchedEffect(playWhenReady) {
        if (isReady.value) {
            if (playWhenReady) jsVideoPlay() else jsVideoPause()
        }
    }

    LaunchedEffect(isReady.value) {
        if (!isReady.value) return@LaunchedEffect
        while (true) {
            val raw = jsVideoGetSnapshot()
            if (raw.isNotBlank()) {
                if (raw.contains("\"ended\":true") && !hasEnded.value) {
                    hasEnded.value = true
                    latestOnEnded.value()
                }
                if (raw.contains("\"error\":") && !raw.contains("\"error\":null") && !raw.contains("\"error\":\"\"")) {
                    latestOnError.value()
                }
            }
            val bytes = jsVideoCaptureFrame(0.5)
            if (bytes.isNotEmpty()) {
                try {
                    val skiaImage = SkiaImage.makeFromEncoded(bytes)
                    currentFrame.value = skiaImage.toComposeImageBitmap()
                } catch (_: Exception) { }
            }
            delay(40)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            currentFrame.value = null
            jsVideoDestroy()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val frame = currentFrame.value ?: return@Canvas
            val dstW = size.width.toInt()
            val dstH = size.height.toInt()
            if (dstW <= 0 || dstH <= 0) return@Canvas
            val srcW = frame.width
            val srcH = frame.height
            if (srcW <= 0 || srcH <= 0) return@Canvas
            val srcRatio = srcW.toFloat() / srcH.toFloat()
            val dstRatio = dstW.toFloat() / dstH.toFloat()
            val (drawW, drawH) = if (srcRatio > dstRatio) {
                dstW.toFloat() to (dstW.toFloat() / srcRatio)
            } else {
                (dstH.toFloat() * srcRatio) to dstH.toFloat()
            }
            drawImage(
                image = frame,
                dstOffset = IntOffset(((dstW - drawW) / 2f).toInt(), ((dstH - drawH) / 2f).toInt()),
                dstSize = IntSize(drawW.toInt(), drawH.toInt()),
            )
        }
    }
}
