package com.nuvio.app.features.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.nuvio.app.features.home.HomeCatalogDefinition
import com.nuvio.app.features.home.Top10CatalogRepository
import com.nuvio.app.features.home.Top10CatalogUiState

internal fun LazyListScope.top10CatalogSettingsContent(
    isTablet: Boolean,
    uiState: Top10CatalogUiState,
    availableCatalogs: List<HomeCatalogDefinition>,
) {
    item {
        SettingsSection(
            title = "General",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = "Enable Top 10 Row",
                    description = "Show a Top 10 section on the home screen",
                    checked = uiState.enabled,
                    isTablet = isTablet,
                    onCheckedChange = { Top10CatalogRepository.setEnabled(it) },
                )
            }
        }
    }

    if (uiState.enabled) {
        item {
            SettingsSection(
                title = "Catalogs",
                isTablet = isTablet,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    CatalogPickerRow(
                        title = "Movies",
                        selectedTitle = uiState.movieCatalogTitle.ifBlank { null },
                        icon = Icons.Rounded.Movie,
                        isTablet = isTablet,
                        availableCatalogs = availableCatalogs,
                        onSelect = { catalog ->
                            Top10CatalogRepository.setMovieCatalog(
                                manifestUrl = catalog.manifestUrl,
                                catalogId = catalog.catalogId,
                                type = catalog.type,
                                title = catalog.defaultTitle,
                            )
                        },
                        onClear = { Top10CatalogRepository.clearMovieCatalog() },
                    )
                    CatalogPickerRow(
                        title = "TV Shows",
                        selectedTitle = uiState.seriesCatalogTitle.ifBlank { null },
                        icon = Icons.Rounded.Tv,
                        isTablet = isTablet,
                        availableCatalogs = availableCatalogs,
                        onSelect = { catalog ->
                            Top10CatalogRepository.setSeriesCatalog(
                                manifestUrl = catalog.manifestUrl,
                                catalogId = catalog.catalogId,
                                type = catalog.type,
                                title = catalog.defaultTitle,
                            )
                        },
                        onClear = { Top10CatalogRepository.clearSeriesCatalog() },
                    )
                }
            }
        }
    }
}