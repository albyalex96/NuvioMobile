package com.nuvio.app.features.cast

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual object CastController {
    private val _state = MutableStateFlow(CastUiState())

    actual val state: StateFlow<CastUiState> = _state.asStateFlow()

    actual fun showCastDialog() {
    }
}

@Composable
actual fun rememberInitCastButton() {
}
