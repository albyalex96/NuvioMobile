package com.nuvio.app.features.cast

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

data class CastUiState(
    val isAvailable: Boolean = false,
    val isConnected: Boolean = false,
    val isCasting: Boolean = false,
)

expect object CastController {
    val state: StateFlow<CastUiState>
    fun showCastDialog()
    fun castMedia(
        url: String,
        title: String,
        mimeType: String? = null,
        posterUrl: String? = null,
        positionMs: Long = 0L,
        sourceHeaders: Map<String, String> = emptyMap(),
    )
    fun stopCasting()
}

@Composable
expect fun rememberInitCastButton()
