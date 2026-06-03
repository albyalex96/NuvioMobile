package com.nuvio.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter

@Composable
actual fun appIconPainter(icon: AppIconResource): Painter = ColorPainter(Color.Gray)
