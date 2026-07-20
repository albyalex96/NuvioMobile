package com.nuvio.app.features.downloads

import androidx.compose.runtime.Composable

@Composable
expect fun rememberDownloadFileSaver(): (DownloadItem) -> Unit
