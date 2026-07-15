package com.nuvio.app.features.cloudstream

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual object CloudStreamRepository {
    private val _uiState = MutableStateFlow(CloudStreamUiState())
    actual val uiState: StateFlow<CloudStreamUiState> = _uiState.asStateFlow()

    actual fun initialize() = Unit
    actual fun onProfileChanged(profileId: Int) = Unit
    actual fun clearLocalState() = Unit
    actual fun acceptSecurityWarning() = Unit
    actual fun toggleGroupByRepository() = Unit

    actual suspend fun addRepository(rawUrl: String): AddCloudStreamRepositoryResult =
        AddCloudStreamRepositoryResult.Error("CloudStream not supported on desktop")
    actual fun refreshRepository(manifestUrl: String) = Unit
    actual fun refreshAll() = Unit
    actual fun removeRepository(manifestUrl: String) = Unit

    actual suspend fun installPlugin(pluginId: String): CloudStreamInstallResult =
        CloudStreamInstallResult.Error("CloudStream not supported on desktop")
    actual suspend fun updatePlugin(pluginId: String): CloudStreamInstallResult =
        CloudStreamInstallResult.Error("CloudStream not supported on desktop")
    actual suspend fun installAndEnablePlugins(pluginIds: List<String>): CloudStreamBulkInstallResult =
        CloudStreamBulkInstallResult(0, 0, 0, 0)
    actual fun setPluginEnabled(pluginId: String, enabled: Boolean) = Unit
    actual fun removePlugin(pluginId: String) = Unit

    actual suspend fun getMainPage(providerId: String, page: Int): Result<List<Pair<String, List<CloudStreamSearchItem>>>> =
        Result.failure(UnsupportedOperationException("CloudStream not supported on desktop"))
    actual suspend fun search(query: String, providerId: String?): List<Result<List<CloudStreamSearchItem>>> = emptyList()
    actual suspend fun loadByExternalId(providerId: String, externalId: String): Result<CloudStreamLoadItem?> =
        Result.failure(UnsupportedOperationException("CloudStream not supported on desktop"))
    actual suspend fun load(providerId: String, data: String): Result<CloudStreamLoadItem> =
        Result.failure(UnsupportedOperationException("CloudStream not supported on desktop"))
    actual suspend fun loadLinks(providerId: String, data: String): Result<List<CloudStreamPlaybackSource>> =
        Result.failure(UnsupportedOperationException("CloudStream not supported on desktop"))
}
