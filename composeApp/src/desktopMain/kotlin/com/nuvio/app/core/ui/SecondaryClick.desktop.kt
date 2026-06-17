package com.nuvio.app.core.ui

import androidx.compose.ui.Modifier

internal fun Modifier.platformSecondaryClick(
    enabled: Boolean,
    onSecondaryClick: () -> Unit,
): Modifier = this
