package com.nuvio.app.features.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize

@Composable
actual fun LockPlayerToLandscape() {
    // No-op on desktop
}

@Composable
actual fun EnterImmersivePlayerMode(keepScreenAwake: Boolean) {
    // No-op on desktop
}

@Composable
actual fun ManagePlayerPictureInPicture(
    isPlaying: Boolean,
    playerSize: IntSize,
) {
    // No-op on desktop (PiP not supported)
}

@Composable
actual fun rememberPlayerGestureController(): PlayerGestureController? = null
