package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.mal.MalAuthRepository
import com.nuvio.app.features.mal.MalAuthUiState
import com.nuvio.app.features.mal.MalConnectionMode
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_cancel
import nuvio.composeapp.generated.resources.settings_mal_attribution_body
import nuvio.composeapp.generated.resources.settings_mal_attribution_title
import nuvio.composeapp.generated.resources.settings_mal_connected_as
import nuvio.composeapp.generated.resources.settings_mal_connect
import nuvio.composeapp.generated.resources.settings_mal_disconnect
import nuvio.composeapp.generated.resources.settings_mal_finish_sign_in
import nuvio.composeapp.generated.resources.settings_mal_not_configured
import nuvio.composeapp.generated.resources.settings_mal_section_authentication
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.malSettingsContent(
    isTablet: Boolean,
    uiState: MalAuthUiState,
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_mal_attribution_title),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                MalAttributionRow(isTablet = isTablet)
            }
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_mal_section_authentication),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                MalConnectionCard(
                    isTablet = isTablet,
                    uiState = uiState,
                )
            }
        }
    }
}

@Composable
private fun MalAttributionRow(isTablet: Boolean) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    ) {
        Text(
            text = stringResource(Res.string.settings_mal_attribution_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(Res.string.settings_mal_attribution_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun MalConnectionCard(
    isTablet: Boolean,
    uiState: MalAuthUiState,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!uiState.credentialsConfigured) {
            Text(
                text = stringResource(Res.string.settings_mal_not_configured),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
            )
            return
        }

        when (uiState.mode) {
            MalConnectionMode.DISCONNECTED -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = {
                            MalAuthRepository.onConnectRequested()
                        },
                    ) {
                        Text(stringResource(Res.string.settings_mal_connect))
                    }
                }
            }

            MalConnectionMode.AWAITING_APPROVAL -> {
                if (uiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Text(
                    text = stringResource(Res.string.settings_mal_finish_sign_in),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { MalAuthRepository.onCancelAuthorization() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }

            MalConnectionMode.CONNECTED -> {
                val username = uiState.username
                if (username != null) {
                    Text(
                        text = stringResource(Res.string.settings_mal_connected_as, username),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                }

                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                OutlinedButton(
                    onClick = { MalAuthRepository.onDisconnectRequested() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(Res.string.settings_mal_disconnect))
                }
            }
        }

        if (uiState.errorMessage != null && uiState.mode != MalConnectionMode.CONNECTED) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = uiState.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
