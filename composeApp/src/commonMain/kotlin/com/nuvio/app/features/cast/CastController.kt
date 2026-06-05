package com.nuvio.app.features.cast

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

data class CastUiState(
    val isAvailable: Boolean = false,
    val isConnected: Boolean = false,
)

expect object CastController {
    val state: StateFlow<CastUiState>
    fun showCastDialog()
}

@Composable
expect fun rememberInitCastButton()
