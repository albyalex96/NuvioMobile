package com.nuvio.app.features.cast

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

actual object CastController {
    private val _state = MutableStateFlow(CastUiState())
    private var mediaRouteButton: MediaRouteButton? = null
    private var castContext: CastContext? = null

    actual val state: StateFlow<CastUiState> = _state.asStateFlow()

    fun initialize(context: Context) {
        try {
            val cc = CastContext.getSharedInstance(context)
            castContext = cc
            Log.d("CastController", "CastContext initialized, state=${cc.castState}")

            cc.addCastStateListener { castState ->
                val available = castState != CastState.NO_DEVICES_AVAILABLE
                val connected = castState == CastState.CONNECTED
                val wasCasting = _state.value.isCasting
                Log.d("CastController", "Cast state changed: isAvailable=$available, isConnected=$connected, wasCasting=$wasCasting")
                _state.value = CastUiState(
                    isAvailable = available,
                    isConnected = connected,
                    isCasting = wasCasting && connected,
                )
            }
            val isAvailable = cc.castState != CastState.NO_DEVICES_AVAILABLE
            val isConnected = cc.castState == CastState.CONNECTED
            Log.d("CastController", "Initial state: isAvailable=$isAvailable, isConnected=$isConnected")
            _state.value = CastUiState(
                isAvailable = isAvailable,
                isConnected = isConnected,
            )
        } catch (e: Exception) {
            Log.w("CastController", "Failed to initialize Cast", e)
            _state.value = CastUiState()
        }
    }

    internal fun createMediaRouteButton(context: Context): MediaRouteButton {
        return MediaRouteButton(context).apply {
            try {
                CastButtonFactory.setUpMediaRouteButton(context, this)
            } catch (_: Exception) { }
            layoutParams = ViewGroup.LayoutParams(1, 1)
            visibility = View.GONE
            mediaRouteButton = this
        }
    }

    actual fun showCastDialog() {
        mediaRouteButton?.performClick()
    }

    actual fun castMedia(
        url: String,
        title: String,
        mimeType: String?,
        posterUrl: String?,
        positionMs: Long,
        sourceHeaders: Map<String, String>,
    ) {
        val castSession = castContext?.sessionManager?.currentCastSession
        val remoteMediaClient = castSession?.remoteMediaClient
        if (remoteMediaClient == null) {
            Log.w("CastController", "Cannot cast: no Cast session or remoteMediaClient")
            return
        }

        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
        metadata.putString(MediaMetadata.KEY_TITLE, title)
        if (!posterUrl.isNullOrBlank()) {
            try {
                metadata.addImage(com.google.android.gms.common.images.WebImage(Uri.parse(posterUrl)))
            } catch (_: Exception) { }
        }

        val contentType = when {
            mimeType != null -> mimeType
            url.contains(".m3u8", ignoreCase = true) -> "application/x-mpegURL"
            url.contains(".mpd", ignoreCase = true) -> "application/dash+xml"
            url.contains(".ts", ignoreCase = true) -> "video/MP2T"
            url.contains(".aac", ignoreCase = true) -> "audio/aac"
            url.contains(".ac3", ignoreCase = true) || url.contains(".eac3", ignoreCase = true) -> "audio/ac3"
            else -> "video/mp4"
        }

        val customData = JSONObject()
        if (sourceHeaders.isNotEmpty()) {
            try {
                val headersJson = JSONObject()
                sourceHeaders.forEach { (k, v) -> headersJson.put(k, v) }
                customData.put("sourceHeaders", headersJson)
            } catch (_: Exception) { }
        }

        val mediaInfo = MediaInfo.Builder(url)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(contentType)
            .setMetadata(metadata)
            .apply {
                if (customData.length() > 0) setCustomData(customData)
            }
            .build()

        val requestData = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .setCurrentTime(positionMs / 1000L)
            .build()

        Log.d("CastController", "Loading media on Cast device: $title ($contentType) at ${positionMs}ms")
        Log.d("CastController", "Cast URL: $url")
        if (sourceHeaders.isNotEmpty()) {
            Log.d("CastController", "Cast sourceHeaders: $sourceHeaders")
        }
        remoteMediaClient.load(requestData)
            .setResultCallback { result ->
                if (result.status.isSuccess()) {
                    Log.d("CastController", "Media loaded successfully on Cast device")
                    _state.value = _state.value.copy(isCasting = true)
                } else {
                    Log.w("CastController", "Failed to load media on Cast device: statusCode=${result.status.statusCode}")
                }
            }
    }

    actual fun stopCasting() {
        val remoteMediaClient = castContext
            ?.sessionManager
            ?.currentCastSession
            ?.remoteMediaClient
        if (remoteMediaClient != null) {
            Log.d("CastController", "Stopping Cast playback")
            remoteMediaClient.stop()
        }
        _state.value = _state.value.copy(isCasting = false)
    }
}

@Composable
actual fun rememberInitCastButton() {
    AndroidView(
        factory = { context -> CastController.createMediaRouteButton(context) },
        modifier = Modifier.size(0.dp),
    )
}
