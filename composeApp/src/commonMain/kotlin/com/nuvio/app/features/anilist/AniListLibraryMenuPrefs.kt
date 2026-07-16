package com.nuvio.app.features.anilist

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class AniListSortBy { LAST_UPDATED, SCORE, TITLE, RELEASE_DATE }

@Serializable
data class AniListLibraryMenuPrefsState(
    val sortBy: AniListSortBy = AniListSortBy.LAST_UPDATED,
    val sortAscending: Boolean = false,
    val openByCatalogUrl: String? = null  // null = title-search fallback
)

object AniListLibraryMenuPrefs {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _state = MutableStateFlow(AniListLibraryMenuPrefsState())
    val state: StateFlow<AniListLibraryMenuPrefsState> = _state.asStateFlow()

    private var loaded = false

    fun ensureLoaded() {
        if (loaded) return
        loaded = true
        runCatching {
            val raw = AniListStorage.loadMenuPrefsPayload().orEmpty().trim()
            if (raw.isNotBlank()) _state.value = json.decodeFromString(raw)
        }
    }

    fun setSortBy(sortBy: AniListSortBy) {
        ensureLoaded()
        if (_state.value.sortBy == sortBy) return
        _state.value = _state.value.copy(sortBy = sortBy)
        persist()
    }

    fun setSortAscending(ascending: Boolean) {
        ensureLoaded()
        if (_state.value.sortAscending == ascending) return
        _state.value = _state.value.copy(sortAscending = ascending)
        persist()
    }

    fun setOpenByCatalogUrl(url: String?) {
        ensureLoaded()
        if (_state.value.openByCatalogUrl == url) return
        _state.value = _state.value.copy(openByCatalogUrl = url)
        persist()
    }

    private fun persist() {
        runCatching { AniListStorage.saveMenuPrefsPayload(json.encodeToString(_state.value)) }
    }
}
