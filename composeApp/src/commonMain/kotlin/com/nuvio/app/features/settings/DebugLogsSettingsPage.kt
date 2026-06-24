package com.nuvio.app.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.logging.InAppLogLevel
import com.nuvio.app.core.logging.InAppLogger
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_advanced_debug_logs
import nuvio.composeapp.generated.resources.settings_advanced_debugging_clear_logs
import nuvio.composeapp.generated.resources.settings_advanced_debugging_clear_logs_description
import nuvio.composeapp.generated.resources.settings_advanced_debugging_copy_logs
import nuvio.composeapp.generated.resources.settings_advanced_debugging_copy_logs_description
import nuvio.composeapp.generated.resources.settings_advanced_debugging_empty
import nuvio.composeapp.generated.resources.settings_advanced_debugging_filter_all
import nuvio.composeapp.generated.resources.settings_advanced_debugging_filter_category
import nuvio.composeapp.generated.resources.settings_advanced_debugging_filter_level
import nuvio.composeapp.generated.resources.settings_advanced_debugging_log_viewer_description
import nuvio.composeapp.generated.resources.settings_advanced_debugging_no_filter_matches
import nuvio.composeapp.generated.resources.settings_advanced_debugging_showing_logs
import nuvio.composeapp.generated.resources.settings_advanced_section_debugging
import org.jetbrains.compose.resources.stringResource

private const val ALL_FILTER_VALUE = "__all__"
private const val MAX_DISPLAYED_LOG_LINES = 500

internal fun LazyListScope.debugLogsSettingsContent(
    isTablet: Boolean,
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_advanced_section_debugging),
            isTablet = isTablet,
        ) {
            DebugLogsViewer(isTablet = isTablet)
        }
    }
}

@Composable
private fun DebugLogsViewer(isTablet: Boolean) {
    val logEntries by InAppLogger.entries.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    val categories = remember(logEntries) {
        logEntries.map { it.category }.distinct().sorted()
    }

    var selectedCategory by remember { mutableStateOf(ALL_FILTER_VALUE) }
    var selectedLevel by remember { mutableStateOf(ALL_FILTER_VALUE) }

    val filteredEntries = remember(logEntries, selectedCategory, selectedLevel) {
        logEntries.filter { entry ->
            (selectedCategory == ALL_FILTER_VALUE || entry.category == selectedCategory) &&
                (selectedLevel == ALL_FILTER_VALUE || entry.level.label == selectedLevel)
        }
    }

    val displayedEntries = filteredEntries.takeLast(MAX_DISPLAYED_LOG_LINES)
    val logText = displayedEntries.joinToString(separator = "\n") { it.line }

    val viewerText = when {
        logEntries.isEmpty() -> stringResource(Res.string.settings_advanced_debugging_empty)
        filteredEntries.isEmpty() -> stringResource(Res.string.settings_advanced_debugging_no_filter_matches)
        else -> logText
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FilterSection(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it },
            selectedLevel = selectedLevel,
            onLevelSelected = { selectedLevel = it },
            isTablet = isTablet,
        )

        Text(
            text = stringResource(Res.string.settings_advanced_debugging_log_viewer_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = if (isTablet) 20.dp else 16.dp),
        )

        Text(
            text = stringResource(
                Res.string.settings_advanced_debugging_showing_logs,
                displayedEntries.size,
                filteredEntries.size,
                logEntries.size,
                InAppLogger.maxRetainedEntries,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = if (isTablet) 20.dp else 16.dp),
        )

        SettingsGroup(isTablet = isTablet) {
            SettingsNavigationRow(
                title = stringResource(Res.string.settings_advanced_debugging_copy_logs),
                description = stringResource(
                    Res.string.settings_advanced_debugging_copy_logs_description,
                    displayedEntries.size,
                ),
                isTablet = isTablet,
                onClick = {
                    if (displayedEntries.isNotEmpty()) {
                        clipboardManager.setText(AnnotatedString(logText))
                    }
                },
            )
            SettingsGroupDivider(isTablet = isTablet)
            SettingsNavigationRow(
                title = stringResource(Res.string.settings_advanced_debugging_clear_logs),
                description = stringResource(Res.string.settings_advanced_debugging_clear_logs_description),
                isTablet = isTablet,
                onClick = {
                    if (logEntries.isNotEmpty()) {
                        InAppLogger.clear()
                    }
                },
            )
        }

        DebugLogTextPanel(
            text = viewerText,
            isEmpty = logEntries.isEmpty() || filteredEntries.isEmpty(),
            isTablet = isTablet,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    selectedLevel: String,
    onLevelSelected: (String) -> Unit,
    isTablet: Boolean,
) {
    val padding = if (isTablet) 20.dp else 16.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = padding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_advanced_debugging_filter_category),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DebugLogFilterChip(
                label = stringResource(Res.string.settings_advanced_debugging_filter_all),
                selected = selectedCategory == ALL_FILTER_VALUE,
                onClick = { onCategorySelected(ALL_FILTER_VALUE) },
            )
            categories.forEach { category ->
                DebugLogFilterChip(
                    label = category,
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                )
            }
        }

        Text(
            text = stringResource(Res.string.settings_advanced_debugging_filter_level),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val levelLabels = listOf(ALL_FILTER_VALUE) + InAppLogLevel.entries.map { it.label }
            levelLabels.forEach { level ->
                DebugLogFilterChip(
                    label = if (level == ALL_FILTER_VALUE) {
                        stringResource(Res.string.settings_advanced_debugging_filter_all)
                    } else {
                        level
                    },
                    selected = selectedLevel == level,
                    onClick = { onLevelSelected(level) },
                )
            }
        }
    }
}

@Composable
private fun DebugLogFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        ),
    )
}

@Composable
private fun DebugLogTextPanel(
    text: String,
    isEmpty: Boolean,
    isTablet: Boolean,
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val heightModifier = if (isEmpty) Modifier.height(200.dp) else Modifier.height(400.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 20.dp else 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .then(heightModifier)
            .verticalScroll(verticalScrollState)
            .horizontalScroll(horizontalScrollState)
            .padding(12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            ),
            color = if (isEmpty) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            softWrap = false,
        )
    }
}

@Composable
internal fun DebugLogsScreen(
    onBack: () -> Unit,
) {
    NuvioScreen(
        modifier = Modifier.fillMaxSize(),
    ) {
        stickyHeader {
            NuvioScreenHeader(
                title = stringResource(Res.string.settings_advanced_debug_logs),
                onBack = onBack,
            )
        }
        debugLogsSettingsContent(
            isTablet = false,
        )
    }
}
