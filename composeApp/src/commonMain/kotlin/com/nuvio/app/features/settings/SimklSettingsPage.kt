package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.library.LibrarySourceMode
import com.nuvio.app.features.simkl.SimklAuthRepository
import com.nuvio.app.features.simkl.SimklAuthUiState
import com.nuvio.app.features.simkl.SimklConnectionMode
import com.nuvio.app.features.trakt.TraktSettingsRepository
import com.nuvio.app.features.trakt.TraktSettingsUiState
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_simkl_connect
import nuvio.composeapp.generated.resources.settings_simkl_continue_sign_in
import nuvio.composeapp.generated.resources.settings_simkl_disconnect
import androidx.compose.material3.ExperimentalMaterial3Api
import nuvio.composeapp.generated.resources.settings_playback_dialog_close
import nuvio.composeapp.generated.resources.settings_simkl_intro
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.simklSettingsContent(isTablet: Boolean, uiState: SimklAuthUiState, settingsUiState: TraktSettingsUiState) {
    item { SettingsSection(title = "SIMKL", isTablet = isTablet) { SettingsGroup(isTablet = isTablet) { SimklConnectionCard(isTablet, uiState) } } }
    if (uiState.mode == SimklConnectionMode.CONNECTED) {
        item { SettingsSection(title = "SIMKL", isTablet = isTablet) { SettingsGroup(isTablet = isTablet) { SimklLibrarySourceRow(isTablet, settingsUiState) } } }
    }
}

@Composable
private fun SimklLibrarySourceRow(isTablet: Boolean, settingsUiState: TraktSettingsUiState) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    Row(
        modifier = Modifier.fillMaxWidth().then(Modifier.padding(horizontal = horizontalPadding, vertical = 14.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Library Source", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text("Choose which library to use for saving and viewing your collection", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { showDialog = true }) {
            Text(when (settingsUiState.librarySourceMode) {
                LibrarySourceMode.SIMKL -> "SIMKL"
                LibrarySourceMode.LOCAL -> "Nuvio Library"
                else -> "Nuvio Library"
            })
        }
    }
    if (showDialog) SimklLibrarySourceDialog(
        selectedSource = settingsUiState.librarySourceMode,
        onSourceSelected = { TraktSettingsRepository.setLibrarySourceMode(it); showDialog = false },
        onDismiss = { showDialog = false },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimklLibrarySourceDialog(
    selectedSource: LibrarySourceMode,
    onSourceSelected: (LibrarySourceMode) -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Library Source", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                listOf(LibrarySourceMode.SIMKL, LibrarySourceMode.LOCAL).forEach { source ->
                    TextButton(
                        onClick = { onSourceSelected(source) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            when (source) {
                                LibrarySourceMode.SIMKL -> "SIMKL"
                                LibrarySourceMode.LOCAL -> "Nuvio Library"
                                else -> "Nuvio Library"
                            },
                            color = if (source == selectedSource) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(Res.string.settings_playback_dialog_close))
                }
            }
        }
    }
}

@Composable
private fun SimklConnectionCard(isTablet: Boolean, uiState: SimklAuthUiState) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = if (isTablet) 20.dp else 16.dp, vertical = if (isTablet) 18.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.settings_simkl_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        when (uiState.mode) {
            SimklConnectionMode.CONNECTED -> Button(onClick = SimklAuthRepository::onDisconnectRequested) { Text(stringResource(Res.string.settings_simkl_disconnect)) }
            else -> Button(onClick = { SimklAuthRepository.onConnectRequested()?.let { runCatching { uriHandler.openUri(it) } } }, enabled = uiState.credentialsConfigured && !uiState.isLoading) { Text(if (uiState.mode == SimklConnectionMode.AWAITING_APPROVAL) stringResource(Res.string.settings_simkl_continue_sign_in) else stringResource(Res.string.settings_simkl_connect)) }
        }
        uiState.error?.let { Text(it.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}
