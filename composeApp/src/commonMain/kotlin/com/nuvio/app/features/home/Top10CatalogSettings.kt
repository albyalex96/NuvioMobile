package com.nuvio.app.features.home
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.nuvio.app.features.home.HomeRepository
@Serializable
data class Top10CatalogSelection(
    val enabled: Boolean = false,
    val movieManifestUrl: String = "",
    val movieCatalogId: String = "",
    val movieCatalogType: String = "",
    val movieCatalogTitle: String = "",
    val seriesManifestUrl: String = "",
    val seriesCatalogId: String = "",
    val seriesCatalogType: String = "",
    val seriesCatalogTitle: String = "",
)

data class Top10CatalogUiState(
    val enabled: Boolean = false,
    val movieManifestUrl: String = "",
    val movieCatalogId: String = "",
    val movieCatalogType: String = "",
    val movieCatalogTitle: String = "",
    val seriesManifestUrl: String = "",
    val seriesCatalogId: String = "",
    val seriesCatalogType: String = "",
    val seriesCatalogTitle: String = "",
) {
    val hasMovieCatalog: Boolean get() = movieManifestUrl.isNotBlank() && movieCatalogId.isNotBlank()
    val hasSeriesCatalog: Boolean get() = seriesManifestUrl.isNotBlank() && seriesCatalogId.isNotBlank()
}

object Top10CatalogRepository {
    private val _uiState = MutableStateFlow(Top10CatalogUiState())
    val uiState: StateFlow<Top10CatalogUiState> = _uiState.asStateFlow()
    private val _localChangeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    internal val localChangeEvents: SharedFlow<Unit> = _localChangeEvents.asSharedFlow()
    private val json = Json { ignoreUnknownKeys = true }

    fun ensureLoaded() {
        val payload = Top10CatalogStorage.loadPayload() ?: return
        val selection = runCatching { json.decodeFromString<Top10CatalogSelection>(payload) }.getOrNull() ?: return
        _uiState.value = selection.toUiState()
    }

    fun setEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(enabled = enabled)
        persist()
    }

    fun setMovieCatalog(manifestUrl: String, catalogId: String, type: String, title: String) {
        _uiState.value = _uiState.value.copy(
            movieManifestUrl = manifestUrl,
            movieCatalogId = catalogId,
            movieCatalogType = type,
            movieCatalogTitle = title,
        )
        persist()
    }

    fun setSeriesCatalog(manifestUrl: String, catalogId: String, type: String, title: String) {
        _uiState.value = _uiState.value.copy(
            seriesManifestUrl = manifestUrl,
            seriesCatalogId = catalogId,
            seriesCatalogType = type,
            seriesCatalogTitle = title,
        )
        persist()
    }

    fun clearMovieCatalog() {
        _uiState.value = _uiState.value.copy(
            movieManifestUrl = "",
            movieCatalogId = "",
            movieCatalogType = "",
            movieCatalogTitle = "",
        )
        persist()
    }

    fun clearSeriesCatalog() {
        _uiState.value = _uiState.value.copy(
            seriesManifestUrl = "",
            seriesCatalogId = "",
            seriesCatalogType = "",
            seriesCatalogTitle = "",
        )
        persist()
    }

    private fun persist() {
        val selection = _uiState.value.toSelection()
        Top10CatalogStorage.savePayload(json.encodeToString(selection))
        _localChangeEvents.tryEmit(Unit)
    }

    private fun Top10CatalogSelection.toUiState() = Top10CatalogUiState(
        enabled = enabled,
        movieManifestUrl = movieManifestUrl,
        movieCatalogId = movieCatalogId,
        movieCatalogType = movieCatalogType,
        movieCatalogTitle = movieCatalogTitle,
        seriesManifestUrl = seriesManifestUrl,
        seriesCatalogId = seriesCatalogId,
        seriesCatalogType = seriesCatalogType,
        seriesCatalogTitle = seriesCatalogTitle,
    )

    private fun Top10CatalogUiState.toSelection() = Top10CatalogSelection(
        enabled = enabled,
        movieManifestUrl = movieManifestUrl,
        movieCatalogId = movieCatalogId,
        movieCatalogType = movieCatalogType,
        movieCatalogTitle = movieCatalogTitle,
        seriesManifestUrl = seriesManifestUrl,
        seriesCatalogId = seriesCatalogId,
        seriesCatalogType = seriesCatalogType,
        seriesCatalogTitle = seriesCatalogTitle,
    )
}