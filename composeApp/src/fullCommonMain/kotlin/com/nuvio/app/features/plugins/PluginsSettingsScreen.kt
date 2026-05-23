package com.nuvio.app.features.plugins

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioIconActionButton
import com.nuvio.app.core.ui.NuvioInfoBadge
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.NuvioSectionLabel
import com.nuvio.app.core.ui.NuvioSurfaceCard
import com.nuvio.app.features.tmdb.TmdbSettingsRepository
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun PluginsSettingsPageContent(
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        PluginRepository.initialize()
    }

    val uiState by PluginRepository.uiState.collectAsStateWithLifecycle()
    val tmdbSettings by remember {
        TmdbSettingsRepository.ensureLoaded()
        TmdbSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var repositoryUrl by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    var testingScraperId by remember { mutableStateOf<String?>(null) }
    val testResults = remember { mutableStateMapOf<String, List<PluginRuntimeResult>>() }

    val sortedRepos = remember(uiState.repositories) {
        uiState.repositories.sortedBy { it.name.lowercase() }
    }
    val hasTmdbApiKey = tmdbSettings.hasApiKey
    val repositoryNameByUrl = remember(sortedRepos) {
        sortedRepos.associate { it.manifestUrl to it.name }
    }
    val sortedScrapers = remember(uiState.scrapers, repositoryNameByUrl) {
        uiState.scrapers.sortedWith(
            compareBy<PluginScraper>(
                { repositoryNameByUrl[it.repositoryUrl]?.lowercase() ?: it.repositoryUrl.lowercase() },
                { it.name.lowercase() },
            ),
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NuvioSectionLabel(stringResource(Res.string.plugins_section_overview))
        NuvioSurfaceCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NuvioInfoBadge(text = stringResource(Res.string.plugins_badge_repos, sortedRepos.size))
                NuvioInfoBadge(text = stringResource(Res.string.plugins_badge_providers, sortedScrapers.size))
                NuvioInfoBadge(
                    text = if (uiState.pluginsEnabled)
                        stringResource(Res.string.plugins_badge_enabled)
                    else
                        stringResource(Res.string.plugins_badge_disabled),
                )
                NuvioInfoBadge(
                    text = if (hasTmdbApiKey)
                        stringResource(Res.string.plugins_badge_tmdb_set)
                    else
                        stringResource(Res.string.plugins_badge_tmdb_missing),
                )
            }
            if (!hasTmdbApiKey) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.plugins_tmdb_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.plugins_toggle_enable_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(Res.string.plugins_toggle_enable_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = uiState.pluginsEnabled,
                    onCheckedChange = { PluginRepository.setPluginsEnabled(it) },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.plugins_toggle_group_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(Res.string.plugins_toggle_group_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = uiState.groupStreamsByRepository,
                    onCheckedChange = { PluginRepository.setGroupStreamsByRepository(it) },
                )
            }
        }

        NuvioSectionLabel(stringResource(Res.string.plugins_section_add_repository))
        NuvioSurfaceCard {
            NuvioInputField(
                value = repositoryUrl,
                onValueChange = {
                    repositoryUrl = it
                    message = null
                },
                placeholder = stringResource(Res.string.plugins_input_manifest_url),
            )
            Spacer(modifier = Modifier.height(16.dp))
            NuvioPrimaryButton(
                text = if (isAdding)
                    stringResource(Res.string.plugins_button_installing)
                else
                    stringResource(Res.string.plugins_button_install),
                enabled = repositoryUrl.isNotBlank() && !isAdding,
                onClick = {
                    val requested = repositoryUrl.trim()
                    if (requested.isBlank()) {
                        message = "plugins_msg_enter_url" // resolved below via state
                        return@NuvioPrimaryButton
                    }
                    isAdding = true
                    message = null
                    coroutineScope.launch {
                        when (val result = PluginRepository.addRepository(requested)) {
                            is AddPluginRepositoryResult.Success -> {
                                repositoryUrl = ""
                                message = result.repository.name
                            }
                            is AddPluginRepositoryResult.Error -> {
                                message = result.message
                            }
                        }
                        isAdding = false
                    }
                },
            )
            message?.let { text ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        NuvioSectionLabel(stringResource(Res.string.plugins_section_installed))
        if (sortedRepos.isEmpty()) {
            NuvioSurfaceCard {
                Text(
                    text = stringResource(Res.string.plugins_repos_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.plugins_repos_empty_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            sortedRepos.forEach { repo ->
                NuvioSurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = repo.name,
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            repo.version?.let { version ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(Res.string.plugins_repo_version, version),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = repo.manifestUrl,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NuvioIconActionButton(
                                icon = Icons.Rounded.Refresh,
                                contentDescription = stringResource(Res.string.plugins_repo_refresh_cd),
                                tint = MaterialTheme.colorScheme.primary,
                                onClick = { PluginRepository.refreshRepository(repo.manifestUrl, pushAfterRefresh = true) },
                            )
                            NuvioIconActionButton(
                                icon = Icons.Rounded.Delete,
                                contentDescription = stringResource(Res.string.plugins_repo_delete_cd),
                                tint = MaterialTheme.colorScheme.error,
                                onClick = { PluginRepository.removeRepository(repo.manifestUrl) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        NuvioInfoBadge(text = stringResource(Res.string.plugins_badge_providers, repo.scraperCount))
                        if (repo.isRefreshing) {
                            NuvioInfoBadge(text = stringResource(Res.string.plugins_repo_refreshing))
                        }
                    }
                    repo.errorMessage?.let { errorMessage ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        NuvioSectionLabel(stringResource(Res.string.plugins_section_providers))
        if (sortedScrapers.isEmpty()) {
            NuvioSurfaceCard {
                Text(
                    text = stringResource(Res.string.plugins_scrapers_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            sortedScrapers.forEach { scraper ->
                val scraperResults = testResults[scraper.id]
                val isTestingThisScraper = testingScraperId == scraper.id
                val repositoryName = repositoryNameByUrl[scraper.repositoryUrl]
                    ?: scraper.repositoryUrl.fallbackRepositoryLabel()

                NuvioSurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Extension,
                                contentDescription = null,
                                tint = if (scraper.enabled) Color(0xFF68B76A) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = repositoryName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = scraper.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = scraper.description.ifBlank { stringResource(Res.string.plugins_scraper_no_description) },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Switch(
                            checked = scraper.enabled,
                            onCheckedChange = { PluginRepository.toggleScraper(scraper.id, it) },
                            enabled = scraper.manifestEnabled,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        NuvioInfoBadge(text = scraper.supportedTypes.joinToString(" | "))
                        NuvioInfoBadge(text = stringResource(Res.string.plugins_scraper_version, scraper.version))
                        if (!scraper.manifestEnabled) {
                            NuvioInfoBadge(text = stringResource(Res.string.plugins_scraper_disabled_by_repo))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    NuvioPrimaryButton(
                        text = if (isTestingThisScraper)
                            stringResource(Res.string.plugins_button_testing)
                        else
                            stringResource(Res.string.plugins_button_test),
                        enabled = hasTmdbApiKey && !isTestingThisScraper,
                        onClick = {
                            testingScraperId = scraper.id
                            coroutineScope.launch {
                                PluginRepository.testScraper(scraper.id)
                                    .onSuccess { results ->
                                        testResults[scraper.id] = results
                                    }
                                    .onFailure { error ->
                                        testResults[scraper.id] = listOf(
                                            PluginRuntimeResult(
                                                title = "Error",
                                                name = error.message ?: "Provider test failed",
                                                url = "about:error",
                                            ),
                                        )
                                    }
                                testingScraperId = null
                            }
                        },
                    )

                    if (!scraperResults.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(Res.string.plugins_test_results, scraperResults.size),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        scraperResults.take(8).forEach { result ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = result.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = result.url,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun String.fallbackRepositoryLabel(): String {
    val withoutQuery = substringBefore("?")
    val withoutManifest = withoutQuery.removeSuffix("/manifest.json")
    val host = withoutManifest.substringAfter("://", withoutManifest).substringBefore('/')
    return host.ifBlank {
        withoutManifest.substringAfterLast('/').ifBlank { "Plugin repository" }
    }
}