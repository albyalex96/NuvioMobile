package com.nuvio.app.features.cloudflare

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun CloudflareSolverWebView(
    url: String,
    onResolved: () -> Unit,
    modifier: Modifier
)
