package com.nuvio.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.browser.window

@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}
        window.addEventListener("popstate", { onBack() })
        onDispose {
            window.removeEventListener("popstate", { onBack() })
        }
    }
}
