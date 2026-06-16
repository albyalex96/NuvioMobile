package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioActionLabel
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.nuvio
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.custom_user_agent_input_placeholder
import nuvio.composeapp.generated.resources.custom_user_agent_not_set
import nuvio.composeapp.generated.resources.custom_user_agent_override_addons
import nuvio.composeapp.generated.resources.custom_user_agent_override_addons_description
import nuvio.composeapp.generated.resources.custom_user_agent_override_both
import nuvio.composeapp.generated.resources.custom_user_agent_override_both_description
import nuvio.composeapp.generated.resources.custom_user_agent_override_plugins
import nuvio.composeapp.generated.resources.custom_user_agent_override_plugins_description
import nuvio.composeapp.generated.resources.custom_user_agent_save
import nuvio.composeapp.generated.resources.custom_user_agent_section_description
import nuvio.composeapp.generated.resources.custom_user_agent_section_title
import nuvio.composeapp.generated.resources.dns_section_title
import nuvio.composeapp.generated.resources.settings_network_dns_custom
import nuvio.composeapp.generated.resources.settings_network_dns_default
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.networkSettingsContent(
    isTablet: Boolean,
) {
    item {
        val repository = globalNetworkSettingsRepository ?: return@item
        val currentProvider by repository.dnsProvider.collectAsState()
        val currentUserAgent by repository.customUserAgent.collectAsState()
        val overrideForAddons by repository.overrideForAddons.collectAsState()
        val overrideForPlugins by repository.overrideForPlugins.collectAsState()
        val overrideForBoth by repository.overrideForBoth.collectAsState()
        var userAgentDraft by remember(currentUserAgent) { mutableStateOf(currentUserAgent) }

        val tokens = MaterialTheme.nuvio

        Column(
            modifier = Modifier.padding(horizontal = if (isTablet) 24.dp else 0.dp)
        ) {
            SettingsSection(
                title = stringResource(Res.string.dns_section_title),
                isTablet = isTablet
            ) {
                SettingsGroup(isTablet = isTablet) {
                    DnsProvider.entries.forEachIndexed { index, provider ->
                        SettingsRadioRow(
                            title = provider.displayName,
                            description = if (provider == DnsProvider.SYSTEM) stringResource(Res.string.settings_network_dns_default) else stringResource(
                                Res.string.settings_network_dns_custom,
                                provider.name.lowercase()
                            ),
                            selected = currentProvider == provider,
                            onClick = { repository.setDnsProvider(provider) },
                            isTablet = isTablet
                        )
                        if (index < DnsProvider.entries.lastIndex) {
                            SettingsGroupDivider(isTablet = isTablet)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(NuvioTokens.Space.s24))

            SettingsSection(
                title = stringResource(Res.string.custom_user_agent_section_title),
                isTablet = isTablet
            ) {
                Text(
                    text = stringResource(Res.string.custom_user_agent_section_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textMuted,
                    modifier = Modifier.padding(
                        horizontal = if (isTablet) 20.dp else 16.dp,
                        vertical = 12.dp
                    ),
                )

                SettingsGroup(isTablet = isTablet) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = if (isTablet) 20.dp else 16.dp,
                                vertical = 12.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = userAgentDraft,
                            onValueChange = { userAgentDraft = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = {
                                Text(
                                    stringResource(Res.string.custom_user_agent_input_placeholder)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = tokens.colors.borderFocus.copy(alpha = tokens.opacity.strong),
                                unfocusedBorderColor = tokens.colors.borderDefault.copy(alpha = tokens.opacity.medium),
                                focusedContainerColor = tokens.colors.surface,
                                unfocusedContainerColor = tokens.colors.surface,
                            ),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        NuvioActionLabel(
                            text = stringResource(Res.string.custom_user_agent_save),
                            onClick = { repository.setCustomUserAgent(userAgentDraft) },
                        )
                    }

                    SettingsGroupDivider(isTablet = isTablet)

                    Text(
                        text = if (currentUserAgent.isBlank()) {
                            stringResource(Res.string.custom_user_agent_not_set)
                        } else {
                            currentUserAgent
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (currentUserAgent.isBlank()) tokens.colors.textMuted else tokens.colors.textPrimary,
                        modifier = Modifier.padding(
                            horizontal = if (isTablet) 20.dp else 16.dp,
                            vertical = 12.dp
                        ),
                    )

                    SettingsGroupDivider(isTablet = isTablet)

                    SettingsSwitchRow(
                        title = stringResource(Res.string.custom_user_agent_override_addons),
                        description = stringResource(Res.string.custom_user_agent_override_addons_description),
                        checked = overrideForAddons,
                        enabled = !overrideForBoth,
                        isTablet = isTablet,
                        onCheckedChange = { repository.setOverrideForAddons(it) },
                    )

                    SettingsGroupDivider(isTablet = isTablet)

                    SettingsSwitchRow(
                        title = stringResource(Res.string.custom_user_agent_override_plugins),
                        description = stringResource(Res.string.custom_user_agent_override_plugins_description),
                        checked = overrideForPlugins,
                        enabled = !overrideForBoth,
                        isTablet = isTablet,
                        onCheckedChange = { repository.setOverrideForPlugins(it) },
                    )

                    SettingsGroupDivider(isTablet = isTablet)

                    SettingsSwitchRow(
                        title = stringResource(Res.string.custom_user_agent_override_both),
                        description = stringResource(Res.string.custom_user_agent_override_both_description),
                        checked = overrideForBoth,
                        enabled = !overrideForAddons && !overrideForPlugins,
                        isTablet = isTablet,
                        onCheckedChange = { repository.setOverrideForBoth(it) },
                    )
                }
            }
        }
    }
}
