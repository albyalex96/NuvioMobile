package com.nuvio.app.features.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
internal actual fun DownloadLocationPicker(
    onLocationSelected: (uri: String) -> Unit,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onDismiss()
    }
}

internal actual fun formatUriForDisplay(uri: String): String = uri
