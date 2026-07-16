package com.nuvio.app.features.anilist.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.features.anilist.AniListLibraryItem
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AniListEditMediaBottomSheet(
    item: AniListLibraryItem,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onSave: (status: String, progress: Int, score: Double?) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Map existing AniList status (watching, completed, planning, paused, dropped, repeating)
    // to uppercase strings for display & saving.
    val statusOptions = listOf(
        "Planning",
        "Watching",
        "Completed",
        "Paused",
        "Dropped",
        "Repeating"
    )

    var selectedStatus by remember(item) {
        val initial = item.status.lowercase().capitalize()
        mutableStateOf(if (initial == "Current") "Watching" else initial)
    }

    var progress by remember(item) {
        mutableIntStateOf(item.progress)
    }

    var isEditingProgress by remember { mutableStateOf(false) }
    var progressInputText by remember { mutableStateOf(progress.toString()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(progress) {
        if (!isEditingProgress) {
            progressInputText = progress.toString()
        }
    }

    LaunchedEffect(isEditingProgress) {
        if (isEditingProgress) {
            focusRequester.requestFocus()
        }
    }

    var score by remember(item) {
        // AniList score is 0-100, we show 0.0 - 10.0
        val initialScore = item.score?.let { it / 10.0 } ?: 0.0
        mutableStateOf(initialScore)
    }

    val sheetBg = Color(0xFF17171D)
    val cardBg = Color(0xFF22222B)
    val buttonDarkBg = Color(0xFF2E2E3A)
    val lavender = Color(0xFFB7B8FF)
    val textPrimary = Color.White
    val textMuted = Color(0xFFA0A0AB)
    val redText = Color(0xFFFF8B8B)

    NuvioModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = sheetBg,
        contentColor = textPrimary,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        showDragHandle = true,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            // Close Button Row (Top corner close inside sheet context)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(buttonDarkBg, CircleShape)
                        .clip(CircleShape)
                        .clickable(onClick = onDismissRequest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Header Section
            Text(
                text = "Edit Media Entry",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = textMuted
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Status Selector Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statusOptions.forEach { option ->
                    val isActive = option.equals(selectedStatus, ignoreCase = true)
                    val bgColor by animateColorAsState(
                        targetValue = if (isActive) lavender else buttonDarkBg,
                        animationSpec = tween(durationMillis = 200)
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isActive) Color(0xFF17171D) else textPrimary.copy(alpha = 0.75f),
                        animationSpec = tween(durationMillis = 200)
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(bgColor)
                            .clickable { selectedStatus = option }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Progress Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progress",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                )
                Text(
                    text = "$progress / ${item.totalEpisodes ?: "??"}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = textMuted
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minus Button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(cardBg, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { if (progress > 0) progress-- },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "Decrease Progress",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Value Display Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .background(cardBg, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            isEditingProgress = true
                            progressInputText = progress.toString()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isEditingProgress) {
                        BasicTextField(
                            value = progressInputText,
                            onValueChange = { newVal ->
                                val filtered = newVal.filter { it.isDigit() }
                                if (filtered.length <= 5) {
                                    progressInputText = filtered
                                    filtered.toIntOrNull()?.let { parsed ->
                                        val max = item.totalEpisodes ?: Int.MAX_VALUE
                                        progress = parsed.coerceIn(0, max)
                                    }
                                }
                            },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (progressInputText.isEmpty()) {
                                        progressInputText = progress.toString()
                                    }
                                    isEditingProgress = false
                                }
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(textPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .focusRequester(focusRequester)
                        )
                    } else {
                        Text(
                            text = progress.toString(),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        )
                    }
                }

                // Plus Button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(cardBg, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val max = item.totalEpisodes ?: Int.MAX_VALUE
                            if (progress < max) progress++
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Increase Progress",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Score Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Score",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                )
                Text(
                    text = "${(score * 10).toLong() / 10.0} / 10",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = textMuted
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = score.toFloat(),
                onValueChange = { score = it.toDouble() },
                valueRange = 0f..10f,
                steps = 99,
                colors = SliderDefaults.colors(
                    thumbColor = lavender,
                    activeTrackColor = lavender,
                    inactiveTrackColor = cardBg,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Bottom Actions Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Delete Button
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cardBg,
                        contentColor = redText
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = redText
                        )
                    )
                }

                // Save Button
                Button(
                    onClick = {
                        val finalProgress = if (isEditingProgress) {
                            progressInputText.toIntOrNull()?.coerceIn(0, item.totalEpisodes ?: Int.MAX_VALUE) ?: progress
                        } else {
                            progress
                        }
                        onSave(selectedStatus, finalProgress, if (score > 0.0) score else null)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = lavender,
                        contentColor = Color(0xFF17171D)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Text(
                        text = "Save Changes",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF17171D)
                        )
                    )
                }
            }
        }
    }
}
