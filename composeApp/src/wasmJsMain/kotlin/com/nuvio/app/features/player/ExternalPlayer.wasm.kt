package com.nuvio.app.features.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberExternalPlayerLauncher(
    onResult: (ExternalPlaybackResult?) -> Unit,
): (ExternalPlayerIntentResult.Success) -> Boolean = remember {
    { _ -> false }
}

internal actual object ExternalPlayerPlatform {
    actual fun defaultPlayerId(): String? = null
    actual fun availablePlayers(): List<ExternalPlayerApp> = emptyList()

    actual fun open(
        request: ExternalPlayerPlaybackRequest,
        playerId: String?,
    ): ExternalPlayerOpenResult = ExternalPlayerOpenResult.NoPlayerAvailable

    actual fun buildIntent(
        request: ExternalPlayerPlaybackRequest,
        playerId: String?,
    ): ExternalPlayerIntentResult = ExternalPlayerIntentResult.Failed
}
