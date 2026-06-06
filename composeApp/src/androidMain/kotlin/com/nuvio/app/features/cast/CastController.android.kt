package com.nuvio.app.features.cast

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.size
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
            Log.d("CastController", "CastContext initialized, state=${cc.castState}")
            cc.addCastStateListener { castState ->
                val available = castState != CastState.NO_DEVICES_AVAILABLE
                val connected = castState == CastState.CONNECTED
                Log.d("CastController", "Cast state changed: isAvailable=$available, isConnected=$connected")
                _state.value = CastUiState(
                    isAvailable = available,
                    isConnected = connected,
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
}

@Composable
actual fun rememberInitCastButton() {
    AndroidView(
        factory = { context -> CastController.createMediaRouteButton(context) },
        modifier = Modifier.size(0.dp),
    )
}
