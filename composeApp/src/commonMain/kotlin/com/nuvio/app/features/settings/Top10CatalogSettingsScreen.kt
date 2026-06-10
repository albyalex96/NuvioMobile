package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.home.Top10CatalogRepository
import com.nuvio.app.features.home.HomeCatalogDefinition
import com.nuvio.app.features.home.buildHomeCatalogDefinitions
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
@Composable
fun Top10CatalogSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val top10UiState by Top10CatalogRepository.uiState.collectAsStateWithLifecycle()
    val addonsUiState by remember {
        AddonRepository.initialize()
        AddonRepository.uiState
    }.collectAsStateWithLifecycle()

    var availableCatalogs by remember { mutableStateOf(emptyList<HomeCatalogDefinition>()) }
    LaunchedEffect(addonsUiState.addons) {
        availableCatalogs = buildHomeCatalogDefinitions(addonsUiState.addons)
    }

    NuvioScreen(
        modifier = modifier.fillMaxSize(),
    ) {
        stickyHeader {
            NuvioScreenHeader(
                title = stringResource(Res.string.top10_header),
                onBack = onBack,
            )
        }

        top10CatalogSettingsContent(
            isTablet = false,
            uiState = top10UiState,
            availableCatalogs = availableCatalogs,
        )
    }
}