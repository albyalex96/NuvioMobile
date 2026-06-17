package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
internal actual fun DownloadLocationPicker(
    onLocationSelected: (uri: String) -> Unit,
    onDismiss: () -> Unit,
) {
    onDismiss()
}

internal actual fun formatUriForDisplay(uri: String): String = uri
