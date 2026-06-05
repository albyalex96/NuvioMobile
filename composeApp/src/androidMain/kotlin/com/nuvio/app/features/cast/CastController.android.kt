package com.nuvio.app.features.cast

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual object CastController {
    private val _state = MutableStateFlow(CastUiState())
    private var mediaRouteButton: MediaRouteButton? = null
    private var castContext: CastContext? = null

    actual val state: StateFlow<CastUiState> = _state.asStateFlow()

    fun initialize(context: Context) {
        try {
            val cc = CastContext.getSharedInstance(context)
            castContext = cc
            cc.addCastStateListener { castState ->
                _state.value = CastUiState(
                    isAvailable = castState != CastState.NO_DEVICES_AVAILABLE,
                    isConnected = castState == CastState.CONNECTED,
                )
            }
            _state.value = CastUiState(
                isAvailable = cc.castState != CastState.NO_DEVICES_AVAILABLE,
                isConnected = cc.castState == CastState.CONNECTED,
            )
        } catch (_: Exception) {
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
}

@Composable
actual fun rememberInitCastButton() {
    AndroidView(
        factory = { context -> CastController.createMediaRouteButton(context) },
        modifier = Modifier.size(0.dp),
    )
}
