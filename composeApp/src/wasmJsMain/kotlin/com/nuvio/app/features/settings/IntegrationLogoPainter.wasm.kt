package com.nuvio.app.features.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter

@Composable
internal actual fun integrationLogoPainter(logo: IntegrationLogo): Painter = ColorPainter(Color.Gray)
