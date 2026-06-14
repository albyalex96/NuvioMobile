package com.nuvio.app.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.player.sponsorblock.SponsorBlockCategory
import com.nuvio.app.features.player.sponsorblock.SponsorBlockSettingsRepository
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_playback_sponsorblock_auto_skip
import nuvio.composeapp.generated.resources.settings_playback_sponsorblock_auto_skip_description
import nuvio.composeapp.generated.resources.settings_playback_sponsorblock_categories
import nuvio.composeapp.generated.resources.settings_playback_sponsorblock_enabled
import nuvio.composeapp.generated.resources.settings_playback_sponsorblock_enabled_description
import nuvio.composeapp.generated.resources.settings_playback_sponsorblock_privacy_mode
import nuvio.composeapp.generated.resources.settings_playback_sponsorblock_privacy_mode_description
import nuvio.composeapp.generated.resources.settings_playback_sponsorblock_show_notification
import nuvio.composeapp.generated.resources.settings_playback_sponsorblock_show_notification_description
import nuvio.composeapp.generated.resources.settings_playback_sponsorblock_show_skip_button
import nuvio.composeapp.generated.resources.settings_playback_sponsorblock_show_skip_button_description
import org.jetbrains.compose.resources.stringResource

@Composable
fun SponsorBlockSettingsSection(
    isTablet: Boolean,
    modifier: Modifier = Modifier,
) {
    val settings by SponsorBlockSettingsRepository.settings.collectAsState()

    SettingsGroup(isTablet = isTablet, modifier = modifier) {
        SettingsSwitchRow(
            title = stringResource(Res.string.settings_playback_sponsorblock_enabled),
            description = stringResource(Res.string.settings_playback_sponsorblock_enabled_description),
            checked = settings.enabled,
            isTablet = isTablet,
            onCheckedChange = { SponsorBlockSettingsRepository.setEnabled(it) },
        )

        if (settings.enabled) {
            SettingsGroupDivider(isTablet = isTablet)

            SettingsSwitchRow(
                title = stringResource(Res.string.settings_playback_sponsorblock_auto_skip),
                description = stringResource(Res.string.settings_playback_sponsorblock_auto_skip_description),
                checked = settings.autoSkip,
                isTablet = isTablet,
                onCheckedChange = { SponsorBlockSettingsRepository.setAutoSkip(it) },
            )

            SettingsGroupDivider(isTablet = isTablet)

            SettingsSwitchRow(
                title = stringResource(Res.string.settings_playback_sponsorblock_show_skip_button),
                description = stringResource(Res.string.settings_playback_sponsorblock_show_skip_button_description),
                checked = settings.showSkipButton,
                isTablet = isTablet,
                onCheckedChange = { SponsorBlockSettingsRepository.setShowSkipButton(it) },
            )

            SettingsGroupDivider(isTablet = isTablet)

            SettingsSwitchRow(
                title = stringResource(Res.string.settings_playback_sponsorblock_show_notification),
                description = stringResource(Res.string.settings_playback_sponsorblock_show_notification_description),
                checked = settings.showNotification,
                isTablet = isTablet,
                onCheckedChange = { SponsorBlockSettingsRepository.setShowNotification(it) },
            )

            SettingsGroupDivider(isTablet = isTablet)

            SettingsSwitchRow(
                title = stringResource(Res.string.settings_playback_sponsorblock_privacy_mode),
                description = stringResource(Res.string.settings_playback_sponsorblock_privacy_mode_description),
                checked = settings.usePrivacyApi,
                isTablet = isTablet,
                onCheckedChange = { SponsorBlockSettingsRepository.setUsePrivacyApi(it) },
            )

            SettingsGroupDivider(isTablet = isTablet)

            val horizontalPadding = if (isTablet) 20.dp else 16.dp
            Text(
                text = stringResource(Res.string.settings_playback_sponsorblock_categories),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = horizontalPadding, top = 12.dp, bottom = 8.dp),
            )

            SponsorBlockCategory.entries.forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { SponsorBlockSettingsRepository.toggleCategory(category) }
                        .padding(horizontal = horizontalPadding, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = category in settings.categories,
                        onCheckedChange = { SponsorBlockSettingsRepository.toggleCategory(category) },
                    )
                    Text(
                        text = category.displayLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}
