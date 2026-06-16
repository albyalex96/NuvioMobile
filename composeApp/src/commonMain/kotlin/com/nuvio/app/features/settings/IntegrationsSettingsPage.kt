package com.nuvio.app.features.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.foundation.lazy.LazyListScope
import nuvio.composeapp.generated.resources.compose_settings_page_debrid
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_settings_page_live_tv
import nuvio.composeapp.generated.resources.compose_settings_page_mdblist_ratings
import nuvio.composeapp.generated.resources.compose_settings_page_tmdb_enrichment
import nuvio.composeapp.generated.resources.compose_settings_page_trakt
import nuvio.composeapp.generated.resources.compose_settings_page_mal
import nuvio.composeapp.generated.resources.compose_settings_page_kitsu
import nuvio.composeapp.generated.resources.compose_settings_page_anilist
import nuvio.composeapp.generated.resources.compose_settings_page_simkl
import nuvio.composeapp.generated.resources.compose_settings_page_opensubtitles
import nuvio.composeapp.generated.resources.compose_settings_page_subdl
import nuvio.composeapp.generated.resources.compose_settings_root_trakt_description
import nuvio.composeapp.generated.resources.settings_integrations_live_tv_description
import nuvio.composeapp.generated.resources.settings_integrations_mdblist_description
import nuvio.composeapp.generated.resources.settings_integrations_debrid_description
import nuvio.composeapp.generated.resources.settings_integrations_mal_description
import nuvio.composeapp.generated.resources.settings_integrations_kitsu_description
import nuvio.composeapp.generated.resources.settings_integrations_anilist_description
import nuvio.composeapp.generated.resources.settings_integrations_simkl_description
import nuvio.composeapp.generated.resources.settings_integrations_opensubtitles_description
import nuvio.composeapp.generated.resources.settings_integrations_subdl_description
import nuvio.composeapp.generated.resources.settings_integrations_section_title
import nuvio.composeapp.generated.resources.settings_integrations_tmdb_description
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.integrationsContent(
    isTablet: Boolean,
    onDebridClick: () -> Unit,
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
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_integrations_section_title),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_debrid),
                    description = stringResource(Res.string.settings_integrations_debrid_description),
                    icon = Icons.Rounded.CloudQueue,
                    isTablet = isTablet,
                    onClick = onDebridClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
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
                    icon = Icons.Rounded.Bookmark,
                    isTablet = isTablet,
                    onClick = onMalClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_kitsu),
                    description = stringResource(Res.string.settings_integrations_kitsu_description),
                    icon = Icons.Rounded.Movie,
                    isTablet = isTablet,
                    onClick = onKitsuClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_anilist),
                    description = stringResource(Res.string.settings_integrations_anilist_description),
                    icon = Icons.Rounded.Favorite,
                    isTablet = isTablet,
                    onClick = onAnilistClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_simkl),
                    description = stringResource(Res.string.settings_integrations_simkl_description),
                    icon = Icons.Rounded.Sync,
                    isTablet = isTablet,
                    onClick = onSimklClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_opensubtitles),
                    description = stringResource(Res.string.settings_integrations_opensubtitles_description),
                    icon = Icons.Rounded.Language,
                    isTablet = isTablet,
                    onClick = onOpenSubtitlesClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_subdl),
                    description = stringResource(Res.string.settings_integrations_subdl_description),
                    icon = Icons.Rounded.Download,
                    isTablet = isTablet,
                    onClick = onSubdlClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
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
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_live_tv),
                    description = stringResource(Res.string.settings_integrations_live_tv_description),
                    icon = Icons.Rounded.LiveTv,
                    isTablet = isTablet,
                    onClick = onLiveTvClick,
                )
            }
        }
    }
}
