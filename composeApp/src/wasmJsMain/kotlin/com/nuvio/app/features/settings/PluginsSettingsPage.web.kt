package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioIconActionButton
import com.nuvio.app.core.ui.NuvioInfoBadge
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.NuvioSectionLabel
import com.nuvio.app.core.ui.NuvioSurfaceCard
import com.nuvio.app.features.plugins.AddPluginRepositoryResult
import com.nuvio.app.features.plugins.PluginRepository
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.plugins_badge_disabled
import nuvio.composeapp.generated.resources.plugins_badge_enabled
import nuvio.composeapp.generated.resources.plugins_badge_providers
import nuvio.composeapp.generated.resources.plugins_badge_repos
import nuvio.composeapp.generated.resources.plugins_button_install_repo
import nuvio.composeapp.generated.resources.plugins_button_installing
import nuvio.composeapp.generated.resources.plugins_cd_delete_repo
import nuvio.composeapp.generated.resources.plugins_cd_refresh_repo
import nuvio.composeapp.generated.resources.plugins_empty_providers
import nuvio.composeapp.generated.resources.plugins_empty_repos_subtitle
import nuvio.composeapp.generated.resources.plugins_empty_repos_title
import nuvio.composeapp.generated.resources.plugins_enable_globally_desc
import nuvio.composeapp.generated.resources.plugins_enable_globally_title
import nuvio.composeapp.generated.resources.plugins_error_enter_repo_url
import nuvio.composeapp.generated.resources.plugins_input_manifest_placeholder
import nuvio.composeapp.generated.resources.plugins_message_installed
import nuvio.composeapp.generated.resources.plugins_repo_fallback_label
import nuvio.composeapp.generated.resources.plugins_repo_version
import nuvio.composeapp.generated.resources.plugins_section_add_repo
import nuvio.composeapp.generated.resources.plugins_section_installed_repos
import nuvio.composeapp.generated.resources.plugins_section_overview
import nuvio.composeapp.generated.resources.plugins_section_providers
import nuvio.composeapp.generated.resources.plugins_provider_disabled_by_repo
import nuvio.composeapp.generated.resources.plugins_provider_no_description
import nuvio.composeapp.generated.resources.plugins_provider_version
import org.jetbrains.compose.resources.stringResource

internal actual fun LazyListScope.pluginsSettingsContent() {
    item {
        val uiState by PluginRepository.uiState.collectAsStateWithLifecycle()
        val coroutineScope = rememberCoroutineScope()
        var repositoryUrl by rememberSaveable { mutableStateOf("") }
        var message by rememberSaveable { mutableStateOf<String?>(null) }
        var isAdding by remember { mutableStateOf(false) }

        val sortedRepos = remember(uiState.repositories) {
            uiState.repositories.sortedBy { it.name.lowercase() }
        }
        val sortedScrapers = remember(uiState.scrapers) {
            uiState.scrapers.sortedBy { it.name.lowercase() }
        }
        val repoFallbackLabel = stringResource(Res.string.plugins_repo_fallback_label)
        val installedTemplate = stringResource(Res.string.plugins_message_installed)
        val enterRepoUrlError = stringResource(Res.string.plugins_error_enter_repo_url)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Overview
            NuvioSectionLabel(stringResource(Res.string.plugins_section_overview))
            NuvioSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NuvioInfoBadge(text = stringResource(Res.string.plugins_badge_repos, sortedRepos.size))
                    NuvioInfoBadge(text = stringResource(Res.string.plugins_badge_providers, sortedScrapers.size))
                    NuvioInfoBadge(
                        text = if (uiState.pluginsEnabled) {
                            stringResource(Res.string.plugins_badge_enabled)
                        } else {
                            stringResource(Res.string.plugins_badge_disabled)
                        },
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.plugins_enable_globally_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(Res.string.plugins_enable_globally_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = uiState.pluginsEnabled,
                        onCheckedChange = { PluginRepository.setPluginsEnabled(it) },
                    )
                }
            }

            // Add Repository
            NuvioSectionLabel(stringResource(Res.string.plugins_section_add_repo))
            NuvioSurfaceCard {
                NuvioInputField(
                    value = repositoryUrl,
                    onValueChange = { repositoryUrl = it; message = null },
                    placeholder = stringResource(Res.string.plugins_input_manifest_placeholder),
                )
                Spacer(Modifier.height(16.dp))
                NuvioPrimaryButton(
                    text = if (isAdding) {
                        stringResource(Res.string.plugins_button_installing)
                    } else {
                        stringResource(Res.string.plugins_button_install_repo)
                    },
                    enabled = repositoryUrl.isNotBlank() && !isAdding,
                    onClick = {
                        if (repositoryUrl.trim().isBlank()) { message = enterRepoUrlError; return@NuvioPrimaryButton }
                        isAdding = true; message = null
                        coroutineScope.launch {
                            when (val result = PluginRepository.addRepository(repositoryUrl.trim())) {
                                is AddPluginRepositoryResult.Success -> {
                                    repositoryUrl = ""
                                    message = installedTemplate.replace("%1\$s", result.repository.name)
                                }
                                is AddPluginRepositoryResult.Error -> message = result.message
                            }
                            isAdding = false
                        }
                    },
                )
                message?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Installed Repositories
            NuvioSectionLabel(stringResource(Res.string.plugins_section_installed_repos))
            if (sortedRepos.isEmpty()) {
                NuvioSurfaceCard {
                    Text(
                        text = stringResource(Res.string.plugins_empty_repos_title),
                        style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.plugins_empty_repos_subtitle),
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                sortedRepos.forEach { repo ->
                    NuvioSurfaceCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text(text = repo.name, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                repo.version?.let { v ->
                                    Spacer(Modifier.height(6.dp))
                                    Text(text = stringResource(Res.string.plugins_repo_version, v), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                repo.errorMessage?.let { err ->
                                    Spacer(Modifier.height(6.dp))
                                    Text(text = err, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                }
                            }
                            Row {
                                IconButton(onClick = { PluginRepository.refreshRepository(repo.manifestUrl) }) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = stringResource(Res.string.plugins_cd_refresh_repo))
                                }
                                IconButton(onClick = { PluginRepository.removeRepository(repo.manifestUrl) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(Res.string.plugins_cd_delete_repo))
                                }
                            }
                        }
                    }
                }
            }

            // Providers / Scrapers
            if (sortedScrapers.isNotEmpty()) {
                NuvioSectionLabel(stringResource(Res.string.plugins_section_providers))
                val disabledByRepo = stringResource(Res.string.plugins_provider_disabled_by_repo)
                val noDesc = stringResource(Res.string.plugins_provider_no_description)
                sortedScrapers.forEach { scraper ->
                    NuvioSurfaceCard {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(text = scraper.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    text = (if (scraper.description.isNotBlank()) scraper.description else noDesc).let {
                                        stringResource(Res.string.plugins_provider_version, scraper.version, it)
                                    },
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (!scraper.manifestEnabled) {
                                NuvioInfoBadge(text = disabledByRepo)
                            } else {
                                Switch(
                                    checked = scraper.enabled,
                                    onCheckedChange = { PluginRepository.toggleScraper(scraper.id, it) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
