package com.nuvio.app.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.ai.AiAssistantSettings
import androidx.compose.runtime.Composable
import com.nuvio.app.features.ai.AiAssistantSettingsRepository
import com.nuvio.app.features.tracking.WatchProgressSource
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_settings_page_ai_assistant
import nuvio.composeapp.generated.resources.compose_settings_page_anilist
import nuvio.composeapp.generated.resources.compose_settings_page_debrid
import nuvio.composeapp.generated.resources.compose_settings_page_kitsu
import nuvio.composeapp.generated.resources.compose_settings_page_live_tv
import nuvio.composeapp.generated.resources.compose_settings_page_mal
import nuvio.composeapp.generated.resources.compose_settings_page_mdblist_ratings
import nuvio.composeapp.generated.resources.compose_settings_page_opensubtitles
import nuvio.composeapp.generated.resources.compose_settings_page_simkl
import nuvio.composeapp.generated.resources.compose_settings_page_subdl
import nuvio.composeapp.generated.resources.compose_settings_page_telegram
import nuvio.composeapp.generated.resources.compose_settings_page_tmdb_enrichment
import nuvio.composeapp.generated.resources.compose_settings_page_trakt
import nuvio.composeapp.generated.resources.compose_settings_root_trakt_description
import nuvio.composeapp.generated.resources.settings_integrations_ai_description
import nuvio.composeapp.generated.resources.settings_integrations_anilist_description
import nuvio.composeapp.generated.resources.settings_integrations_debrid_description
import nuvio.composeapp.generated.resources.settings_integrations_kitsu_description
import nuvio.composeapp.generated.resources.settings_integrations_live_tv_description
import nuvio.composeapp.generated.resources.settings_integrations_mal_description
import nuvio.composeapp.generated.resources.settings_integrations_mdblist_description
import nuvio.composeapp.generated.resources.settings_integrations_opensubtitles_description
import nuvio.composeapp.generated.resources.settings_integrations_section_extra
import nuvio.composeapp.generated.resources.settings_integrations_section_lists
import nuvio.composeapp.generated.resources.settings_integrations_section_metadata
import nuvio.composeapp.generated.resources.settings_integrations_section_subtitles
import nuvio.composeapp.generated.resources.settings_integrations_section_watchprogress
import nuvio.composeapp.generated.resources.settings_integrations_simkl_description
import nuvio.composeapp.generated.resources.settings_integrations_subdl_description
import nuvio.composeapp.generated.resources.settings_integrations_telegram_description
import nuvio.composeapp.generated.resources.settings_integrations_tmdb_description
import nuvio.composeapp.generated.resources.trakt_watch_progress_source_nuvio
import nuvio.composeapp.generated.resources.trakt_watch_progress_source_trakt
import nuvio.composeapp.generated.resources.trakt_watch_progress_title
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.integrationsContent(
    isTablet: Boolean,
    watchProgressCurrentSource: WatchProgressSource?,
    onWatchProgressClick: () -> Unit,
    onAiAssistantClick: () -> Unit,
    onTraktClick: () -> Unit,
    onMalClick: () -> Unit,
    onKitsuClick: () -> Unit,
    onAnilistClick: () -> Unit,
    onSimklClick: () -> Unit,
    onOpenSubtitlesClick: () -> Unit,
    onSubdlClick: () -> Unit,
    onTmdbClick: () -> Unit,
    onMdbListClick: () -> Unit,
    onLiveTvClick: () -> Unit,
    onDebridClick: () -> Unit,
    onTelegramClick: () -> Unit,
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_integrations_section_watchprogress),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onWatchProgressClick)
                        .padding(
                            horizontal = if (isTablet) 20.dp else 16.dp,
                            vertical = if (isTablet) 16.dp else 14.dp,
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.trakt_watch_progress_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = watchProgressLabel(watchProgressCurrentSource),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_integrations_section_metadata),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_tmdb_enrichment),
                    description = stringResource(Res.string.settings_integrations_tmdb_description),
                    iconPainter = integrationLogoPainter(IntegrationLogo.Tmdb),
                    isTablet = isTablet,
                    onClick = onTmdbClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_mdblist_ratings),
                    description = stringResource(Res.string.settings_integrations_mdblist_description),
                    iconPainter = integrationLogoPainter(IntegrationLogo.MdbList),
                    isTablet = isTablet,
                    onClick = onMdbListClick,
                )
            }
        }
    }
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_integrations_section_lists),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_trakt),
                    description = stringResource(Res.string.compose_settings_root_trakt_description),
                    iconPainter = integrationLogoPainter(IntegrationLogo.Trakt),
                    isTablet = isTablet,
                    onClick = onTraktClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_mal),
                    description = stringResource(Res.string.settings_integrations_mal_description),
                    iconPainter = integrationLogoPainter(IntegrationLogo.Mal),
                    isTablet = isTablet,
                    onClick = onMalClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_kitsu),
                    description = stringResource(Res.string.settings_integrations_kitsu_description),
                    iconPainter = integrationLogoPainter(IntegrationLogo.Kitsu),
                    isTablet = isTablet,
                    onClick = onKitsuClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_anilist),
                    description = stringResource(Res.string.settings_integrations_anilist_description),
                    iconPainter = integrationLogoPainter(IntegrationLogo.Anilist),
                    isTablet = isTablet,
                    onClick = onAnilistClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_simkl),
                    description = stringResource(Res.string.settings_integrations_simkl_description),
                    iconPainter = integrationLogoPainter(IntegrationLogo.Simkl),
                    isTablet = isTablet,
                    onClick = onSimklClick,
                )
            }
        }
    }
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_integrations_section_subtitles),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_opensubtitles),
                    description = stringResource(Res.string.settings_integrations_opensubtitles_description),
                    iconPainter = integrationLogoPainter(IntegrationLogo.OpenSubtitles),
                    isTablet = isTablet,
                    onClick = onOpenSubtitlesClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_subdl),
                    description = stringResource(Res.string.settings_integrations_subdl_description),
                    iconPainter = integrationLogoPainter(IntegrationLogo.Subdl),
                    isTablet = isTablet,
                    onClick = onSubdlClick,
                )
            }
        }
    }
    item {
        val aiAssistantSettings by AiAssistantSettingsRepository.uiState.collectAsState()

        SettingsSection(
            title = stringResource(Res.string.settings_integrations_section_extra),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_ai_assistant),
                    description = stringResource(Res.string.settings_integrations_ai_description),
                    icon = Icons.Rounded.AutoAwesome,
                    isTablet = isTablet,
                    onClick = onAiAssistantClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_live_tv),
                    description = stringResource(Res.string.settings_integrations_live_tv_description),
                    icon = Icons.Rounded.LiveTv,
                    isTablet = isTablet,
                    onClick = onLiveTvClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_debrid),
                    description = stringResource(Res.string.settings_integrations_debrid_description),
                    icon = Icons.Rounded.Cloud,
                    isTablet = isTablet,
                    onClick = onDebridClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_telegram),
                    description = stringResource(Res.string.settings_integrations_telegram_description),
                    icon = Icons.Rounded.Send,
                    isTablet = isTablet,
                    onClick = onTelegramClick,
                )
            }
        }
    }
}

@Composable
private fun watchProgressLabel(source: WatchProgressSource?): String =
    when (source) {
        WatchProgressSource.TRAKT -> stringResource(Res.string.trakt_watch_progress_source_trakt)
        WatchProgressSource.SIMKL -> "SIMKL"
        WatchProgressSource.NUVIO_SYNC -> stringResource(Res.string.trakt_watch_progress_source_nuvio)
        null -> ""
    }
