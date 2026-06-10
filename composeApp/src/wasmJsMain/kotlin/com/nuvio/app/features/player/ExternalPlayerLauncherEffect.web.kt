package com.nuvio.app.features.player

import androidx.compose.runtime.Composable

@Composable
actual fun rememberExternalPlayerLauncher(
    onResult: (ExternalPlaybackResult?) -> Unit,
): (ExternalPlayerIntentResult.Success) -> Boolean = { false }
