package com.nuvio.app.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateDpAsState
import coil3.compose.AsyncImage
import com.nuvio.app.core.i18n.localizedShortMonthName
import com.nuvio.app.core.ui.NuvioActionLabel
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.anilist.AniListAuthRepository
import com.nuvio.app.features.anilist.AniListConnectionMode
import com.nuvio.app.features.anilist.AniListSettingsRepository
import com.nuvio.app.features.anilist.AniListSyncCoordinator
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_anilist_attribution_body
import nuvio.composeapp.generated.resources.settings_anilist_attribution_title
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.roundToInt

internal fun LazyListScope.anilistSettingsContent(
    isTablet: Boolean,
) {
    item {
        SettingsGroup(isTablet = isTablet) {
            AniListBrandIntro(isTablet = isTablet)
        }
    }

    item {
        SettingsSection(
            title = "AniList Integration Settings",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                AniListConnectionCard(isTablet = isTablet)
            }
        }
    }

    item {
        val authUiState by AniListAuthRepository.uiState.collectAsState()
        val settingsUiState by AniListSettingsRepository.uiState.collectAsState()

        if (authUiState.mode == AniListConnectionMode.CONNECTED) {
            Spacer(modifier = Modifier.height(16.dp))
            SettingsSection(
                title = "Playback",
                isTablet = isTablet,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    AniListWatchedThresholdRow(
                        isTablet = isTablet,
                        threshold = settingsUiState.markWatchedThreshold
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = if (isTablet) 24.dp else 16.dp))
                    SettingsSwitchRow(
                        title = "Automatically Add New Anime to AniList",
                        description = "When enabled, watching an anime not in your AniList library will automatically add it to your Watching list and sync your progress.",
                        checked = settingsUiState.autoAddNewAnime,
                        enabled = settingsUiState.enableSync,
                        isTablet = isTablet,
                        onCheckedChange = { AniListSettingsRepository.setAutoAddNewAnime(it) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            SettingsSection(
                title = "Customize Library Sections",
                isTablet = isTablet,
                actions = {
                    NuvioActionLabel(
                        text = "Reset",
                        onClick = {
                            AniListSettingsRepository.resetLibrarySections()
                        }
                    )
                }
            ) {
                AniListSectionsList(
                    isTablet = isTablet,
                    items = settingsUiState.librarySections
                )
            }
        }
    }
}

@Composable
private fun AniListBrandIntro(
    isTablet: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = if (isTablet) 24.dp else 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = stringResource(Res.string.settings_anilist_attribution_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.settings_anilist_attribution_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AniListConnectionCard(
    isTablet: Boolean
) {
    val authUiState by AniListAuthRepository.uiState.collectAsState()
    val settingsUiState by AniListSettingsRepository.uiState.collectAsState()
    val isSyncing by AniListSyncCoordinator.isSyncing.collectAsState()
    val syncMessage by AniListSyncCoordinator.syncMessage.collectAsState()

    val uriHandler = LocalUriHandler.current
    var showDisconnectConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = if (isTablet) 24.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (authUiState.mode) {
            AniListConnectionMode.CONNECTED -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!authUiState.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = authUiState.avatarUrl,
                            contentDescription = authUiState.username,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = authUiState.username?.take(1)?.uppercase() ?: "A",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = authUiState.username ?: "AniList User",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Connected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val expiresAt = authUiState.tokenExpiresAtEpochMs
                        if (expiresAt != null && expiresAt > 0L) {
                            val remainingMs = expiresAt - androidx.compose.runtime.remember {
                                com.nuvio.app.features.watchprogress.WatchProgressClock.nowEpochMs()
                            }
                            val label = when {
                                remainingMs <= 0 -> "Token expired"
                                remainingMs < 60_000L -> "Expires in <1 min"
                                remainingMs < 3_600_000L -> "Expires in ${remainingMs / 60_000L} min"
                                remainingMs < 86_400_000L -> "Expires in ${remainingMs / 3_600_000L}h ${(remainingMs % 3_600_000L) / 60_000L}m"
                                else -> "Expires in ${remainingMs / 86_400_000L}d"
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = { showDisconnectConfirm = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Disconnect")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                SettingsSwitchRow(
                    title = "Enable AniList Sync",
                    description = "Master sync control for AniList watch progress.",
                    checked = settingsUiState.enableSync,
                    isTablet = isTablet,
                    onCheckedChange = { AniListSettingsRepository.setEnableSync(it) }
                )

                SettingsSwitchRow(
                    title = "Sync Watching Progress",
                    description = "Synchronize watching state changes with AniList.",
                    checked = settingsUiState.syncWatching,
                    enabled = settingsUiState.enableSync,
                    isTablet = isTablet,
                    onCheckedChange = { AniListSettingsRepository.setSyncWatching(it) }
                )

                SettingsSwitchRow(
                    title = "Auto Sync While Watching",
                    description = "Upload progress automatically while playing files.",
                    checked = settingsUiState.autoSync,
                    enabled = settingsUiState.enableSync,
                    isTablet = isTablet,
                    onCheckedChange = { AniListSettingsRepository.setAutoSync(it) }
                )

                SettingsSwitchRow(
                    title = "Sync on App Launch",
                    description = "Run a full sync check every time the app opens.",
                    checked = settingsUiState.syncOnLaunch,
                    enabled = settingsUiState.enableSync,
                    isTablet = isTablet,
                    onCheckedChange = { AniListSettingsRepository.setSyncOnLaunch(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (settingsUiState.enableSync) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Button(
                            onClick = { AniListSyncCoordinator.syncNow() },
                            enabled = !isSyncing,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Syncing...")
                            } else {
                                Text("Sync Now")
                            }
                        }

                        if (!syncMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = syncMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (settingsUiState.lastSyncTimestamp > 0L) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Last Synced: Just now",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            AniListConnectionMode.DISCONNECTED -> {
                Text(
                    text = "You are not connected to AniList. Connect your account to synchronize anime shelves and tracking state.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!authUiState.errorMessage.isNullOrBlank()) {
                    Text(
                        text = authUiState.errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val authUrl = AniListAuthRepository.onConnectRequested()
                        runCatching { uriHandler.openUri(authUrl) }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connect AniList")
                }
            }

            AniListConnectionMode.LOADING -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (showDisconnectConfirm) {
        BasicAlertDialog(onDismissRequest = { showDisconnectConfirm = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Disconnect AniList?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Are you sure you want to disconnect your AniList account? This will clear local anime progress syncing settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showDisconnectConfirm = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                AniListAuthRepository.disconnect()
                                showDisconnectConfirm = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text("Disconnect")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AniListSectionSettingsRow(
    item: com.nuvio.app.features.anilist.AniListSectionSettings,
    isTablet: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    dragHandleScope: ReorderableCollectionItemScope,
) {
    val tokens = MaterialTheme.nuvio
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 18.dp else 16.dp
    val hapticFeedback = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.type,
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Switch(
                checked = item.enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = tokens.colors.onAccent,
                    checkedTrackColor = tokens.colors.accent,
                    uncheckedThumbColor = tokens.colors.textMuted,
                    uncheckedTrackColor = tokens.colors.borderDefault,
                ),
            )
            IconButton(
                modifier = with(dragHandleScope) {
                    Modifier.draggableHandle(
                        onDragStarted = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragStopped = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                    )
                },
                onClick = {},
            ) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "Reorder",
                    tint = tokens.colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun AniListSectionsList(
    isTablet: Boolean,
    items: List<com.nuvio.app.features.anilist.AniListSectionSettings>,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
    ) { from, to ->
        AniListSettingsRepository.moveSection(from.index, to.index)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    SettingsGroup(isTablet = isTablet) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (isTablet) 550.dp else 400.dp),
            state = lazyListState,
        ) {
            itemsIndexed(items, key = { _, item -> item.type }) { index, item ->
                ReorderableItem(
                    reorderableLazyListState,
                    key = item.type,
                ) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)

                    Surface(shadowElevation = elevation) {
                        Column {
                            if (index > 0) {
                                SettingsGroupDivider(isTablet = isTablet)
                            }
                            AniListSectionSettingsRow(
                                item = item,
                                isTablet = isTablet,
                                onEnabledChange = { enabled ->
                                    AniListSettingsRepository.setSectionEnabled(item.type, enabled)
                                },
                                dragHandleScope = this@ReorderableItem,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AniListWatchedThresholdRow(
    isTablet: Boolean,
    threshold: Float,
) {
    val percent = (threshold * 100f).roundToInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 24.dp else 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mark Episode as Watched At",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Episode is marked as watched after reaching this point",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = threshold,
            onValueChange = { newValue ->
                AniListSettingsRepository.setMarkWatchedThreshold(newValue)
            },
            valueRange = 0f..1f,
            steps = 19,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "0%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "100%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
