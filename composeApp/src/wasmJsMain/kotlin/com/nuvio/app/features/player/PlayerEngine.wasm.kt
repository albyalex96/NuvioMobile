package com.nuvio.app.features.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

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
    sanitizePlaybackResponseHeaders(sourceResponseHeaders)

    val controller = remember {
        object : PlayerEngineController {
            override fun play() {}
            override fun pause() {}
            override fun seekTo(positionMs: Long) {}
            override fun seekBy(offsetMs: Long) {}
            override fun retry() {}
            override fun setPlaybackSpeed(speed: Float) {}
            override fun getAudioTracks(): List<AudioTrack> = emptyList()
            override fun getSubtitleTracks(): List<SubtitleTrack> = emptyList()
            override fun selectAudioTrack(index: Int) {}
            override fun selectSubtitleTrack(index: Int) {}
            override fun setSubtitleUri(url: String) {}
            override fun clearExternalSubtitle() {}
            override fun clearExternalSubtitleAndSelect(trackIndex: Int) {}
        }
    }

    LaunchedEffect(controller) {
        onControllerReady(controller)
    }

    LaunchedEffect(Unit) {
        onError("Video player not available on web yet")
    }
}
