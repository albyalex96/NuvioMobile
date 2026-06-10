package com.nuvio.app.features.home.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.ui.NuvioPosterCard
import com.nuvio.app.core.ui.NuvioPosterShape
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.PosterShape
import com.nuvio.app.features.library.LibraryRepository

@Composable
fun HomePosterCard(
    item: MetaPreview,
    modifier: Modifier = Modifier,
    useLandscapeBackdropMode: Boolean = false,
    isWatched: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()
    val isLandscapeMode = useLandscapeBackdropMode || posterCardStyle.catalogLandscapeModeEnabled
    LaunchedEffect(Unit) {
        LibraryRepository.ensureLoaded()
    }
    val libraryUiState by LibraryRepository.uiState.collectAsStateWithLifecycle()
    var isSaved by remember(item.id, item.type) { mutableStateOf(false) }
    LaunchedEffect(libraryUiState, item.id, item.type) {
        isSaved = LibraryRepository.isSaved(item.id, item.type)
    }
    var detailLine by remember(item.releaseInfo, isLandscapeMode, posterCardStyle.hideLabelsEnabled) { mutableStateOf<String?>(null) }
    LaunchedEffect(item.releaseInfo, isLandscapeMode, posterCardStyle.hideLabelsEnabled) {
        detailLine = if (isLandscapeMode || posterCardStyle.hideLabelsEnabled) null else item.releaseInfo?.let { formatReleaseDateForDisplay(it) }
    }

    NuvioPosterCard(
        title = item.name,
        imageUrl = if (isLandscapeMode) (item.banner ?: item.poster) else item.poster,
        modifier = modifier,
        shape = if (isLandscapeMode) NuvioPosterShape.Landscape else item.posterShape.toNuvioPosterShape(),
        detailLine = detailLine,
        showTitleBelow = !posterCardStyle.hideLabelsEnabled,
        bottomLeftLogoUrl = if (isLandscapeMode) item.logo else null,
        bottomLeftText = if (isLandscapeMode && item.logo.isNullOrBlank() && !posterCardStyle.hideLabelsEnabled) item.name else null,
        isWatched = isWatched,
        isSaved = isSaved,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

private fun PosterShape.toNuvioPosterShape(): NuvioPosterShape =
    when (this) {
        PosterShape.Poster -> NuvioPosterShape.Poster
        PosterShape.Square -> NuvioPosterShape.Square
        PosterShape.Landscape -> NuvioPosterShape.Landscape
    }
