package com.nuvio.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.color_picker_hex_label
import nuvio.composeapp.generated.resources.color_picker_presets
import nuvio.composeapp.generated.resources.cd_selected
import org.jetbrains.compose.resources.stringResource

private val PRESET_COLORS = listOf(
    "#E53935", "#D81B60", "#8E24AA", "#5E35B1", "#1E88E5",
    "#00ACC1", "#43A047", "#7CB342", "#FDD835", "#FB8C00",
    "#F4511E", "#6D4C41", "#546E7A", "#78909C", "#F5F5F5",
    "#FF6F00", "#00897B", "#3949AB", "#C0CA33", "#E91E63",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NuvioColorPicker(
    currentHex: String,
    onColorChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = remember(currentHex) { parseHexColor(currentHex) ?: Color(0xFF1E88E5) }
    var hsv by remember(color) { mutableStateOf(color.toHsv()) }
    var hexInput by remember(currentHex) { mutableStateOf(currentHex.removePrefix("#")) }
    var isHexValid by remember { mutableStateOf(true) }

    fun updateFromHex(hex: String) {
        val clean = hex.trim().removePrefix("#")
        if (clean.length == 6 && clean.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }) {
            val parsed = parseHexColor("#$clean")
            if (parsed != null) {
                hsv = parsed.toHsv()
                isHexValid = true
                hexInput = clean.uppercase()
                onColorChanged("#${clean.uppercase()}")
                return
            }
        }
        isHexValid = hex.isBlank() || hex.length < 6
    }

    val currentColor = remember(hsv) { Color.fromHsv(hsv[0], hsv[1], hsv[2]) }
    val currentHexString = remember(currentColor) { currentColor.toRgbHex() }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(currentColor)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
            )

            OutlinedTextField(
                value = hexInput,
                onValueChange = { hexInput = it.uppercase(); updateFromHex(it) },
                label = { Text(stringResource(Res.string.color_picker_hex_label)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                prefix = { Text("#", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                isError = !isHexValid,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = currentColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    cursorColor = currentColor,
                    focusedLabelColor = currentColor,
                ),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.color_picker_presets),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PRESET_COLORS.forEach { presetHex ->
                val presetColor = remember(presetHex) { parseHexColor(presetHex) ?: Color.Gray }
                val isSelected = presetHex.equals(currentHexString, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(presetColor)
                        .then(
                            if (isSelected) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            } else {
                                Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                            }
                        )
                        .clickable {
                            val parsed = parseHexColor(presetHex) ?: return@clickable
                            hsv = parsed.toHsv()
                            hexInput = presetHex.removePrefix("#")
                            isHexValid = true
                            onColorChanged(presetHex)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = stringResource(Res.string.cd_selected),
                            tint = if (presetColor.luminance() > 0.5f) Color(0xFF111111) else Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ColorSlider(
            label = "H",
            value = hsv[0],
            onValueChange = { hsv = floatArrayOf(it, hsv[1], hsv[2]); updateFromHex(currentColor.toRgbHex().removePrefix("#")) },
            valueRange = 0f..360f,
            gradientStops = listOf(
                0f to Color.Red,
                0.166f to Color.Yellow,
                0.333f to Color.Green,
                0.5f to Color.Cyan,
                0.666f to Color.Blue,
                0.833f to Color.Magenta,
                1f to Color.Red,
            ),
            thumbColor = Color.fromHsv(hsv[0], 100f, 100f),
        )

        Spacer(modifier = Modifier.height(12.dp))

        ColorSlider(
            label = "S",
            value = hsv[1],
            onValueChange = { hsv = floatArrayOf(hsv[0], it, hsv[2]); updateFromHex(currentColor.toRgbHex().removePrefix("#")) },
            valueRange = 0f..100f,
            gradientStops = listOf(
                0f to Color(0xFF666666),
                1f to Color.fromHsv(hsv[0], 100f, hsv[2].coerceAtLeast(30f)),
            ),
            thumbColor = currentColor,
        )

        Spacer(modifier = Modifier.height(12.dp))

        ColorSlider(
            label = "V",
            value = hsv[2],
            onValueChange = { hsv = floatArrayOf(hsv[0], hsv[1], it); updateFromHex(currentColor.toRgbHex().removePrefix("#")) },
            valueRange = 0f..100f,
            gradientStops = listOf(
                0f to Color.Black,
                1f to Color.fromHsv(hsv[0], hsv[1], 100f),
            ),
            thumbColor = currentColor,
        )
    }
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    gradientStops: List<Pair<Float, Color>>,
    thumbColor: Color,
) {
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    val density = androidx.compose.ui.platform.LocalDensity.current
    var widthPx by remember { mutableStateOf(0f) }
    val thumbRadius = 14.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(16.dp),
            textAlign = TextAlign.Center,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .onSizeChanged { widthPx = it.width.toFloat() }
                .pointerInput(valueRange) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val f = (offset.x / size.width).coerceIn(0f, 1f)
                            val newValue = valueRange.start + f * (valueRange.endInclusive - valueRange.start)
                            onValueChange(newValue)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val f = (change.position.x / size.width).coerceIn(0f, 1f)
                            val newValue = valueRange.start + f * (valueRange.endInclusive - valueRange.start)
                            onValueChange(newValue)
                        },
                    )
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(*gradientStops.toTypedArray())
                    ),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(
                        x = with(density) {
                            val totalDp = widthPx.toDp()
                            val usable = totalDp - thumbRadius * 2
                            if (usable > 0.dp) usable * fraction else 0.dp
                        },
                        y = 0.dp,
                    )
                    .size(thumbRadius * 2)
                    .clip(CircleShape)
                    .background(thumbColor)
                    .border(2.dp, Color.White, CircleShape),
            )
        }
    }
}
