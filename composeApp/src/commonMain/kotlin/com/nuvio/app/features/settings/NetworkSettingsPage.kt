package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nuvio.composeapp.generated.resources.dns_section_title
import nuvio.composeapp.generated.resources.settings_network_dns_default
import nuvio.composeapp.generated.resources.settings_network_dns_custom
import nuvio.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
internal fun LazyListScope.networkSettingsContent(
    isTablet: Boolean,
) {
    item {
        val repository = globalNetworkSettingsRepository ?: return@item
        val currentProvider by repository.dnsProvider.collectAsState()

        androidx.compose.foundation.layout.Column(
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
        }
    }
}
