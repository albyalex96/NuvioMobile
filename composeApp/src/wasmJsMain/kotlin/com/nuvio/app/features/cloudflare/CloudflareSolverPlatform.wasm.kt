package com.nuvio.app.features.cloudflare

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun CloudflareSolverWebView(
    url: String,
    onResolved: () -> Unit,
    modifier: Modifier,
) {
    // Cloudflare solving not available on web
}
