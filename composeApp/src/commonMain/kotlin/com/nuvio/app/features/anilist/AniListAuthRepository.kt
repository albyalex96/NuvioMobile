package com.nuvio.app.features.anilist

import co.touchlab.kermit.Logger
import com.nuvio.app.features.watchprogress.WatchProgressClock
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_settings_page_anilist

object AniListAuthRepository {
    private val log = Logger.withTag("AniListAuth")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(AniListAuthUiState())
    val uiState: StateFlow<AniListAuthUiState> = _uiState.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private var hasLoaded = false
    private var authState = AniListAuthState()

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun clearLocalState() {
        hasLoaded = false
        authState = AniListAuthState()
        _isAuthenticated.value = false
        _uiState.value = AniListAuthUiState()
        persist()
    }

    fun onConnectRequested(): String {
        ensureLoaded()
        // We use response_type=code as the primary flow since we have the client secret, 
        // but our callback handler will fallback to implicit token if needed.
        return "https://anilist.co/api/v2/oauth/authorize?client_id=${AniListConfig.CLIENT_ID}&redirect_uri=${AniListConfig.REDIRECT_URI}&response_type=code"
    }

    fun onAuthCallbackReceived(callbackUrl: String) {
        ensureLoaded()
        if (!callbackUrl.startsWith(AniListConfig.REDIRECT_URI, ignoreCase = true)) {
            return
        }

        scope.launch {
            publish(isLoading = true, errorMessage = null)
            
            // Normalize fragment (#) to query (?) for easy parsing
            val normalizedUrlString = if (callbackUrl.contains("#")) {
                callbackUrl.replace("#", "?")
            } else {
                callbackUrl
            }

            val parsedUrl = runCatching { Url(normalizedUrlString) }
                .onFailure {
                    log.w { "Invalid AniList callback URL: ${it.message}" }
                }
                .getOrNull()

            if (parsedUrl == null) {
                publish(isLoading = false, errorMessage = "Invalid callback URL received.")
                return@launch
            }

            val error = parsedUrl.parameters["error"]
            if (!error.isNullOrBlank()) {
                val errorDesc = parsedUrl.parameters["error_description"] ?: "Authorization denied by user."
                publish(isLoading = false, errorMessage = errorDesc)
                return@launch
            }

            // Path 1: Check for implicit access token
            val implicitToken = parsedUrl.parameters["access_token"]
            if (!implicitToken.isNullOrBlank()) {
                val expiresInSeconds = parsedUrl.parameters["expires_in"]?.toLongOrNull() ?: 31536000L
                completeAuthWithToken(implicitToken, null, expiresInSeconds)
                return@launch
            }

            // Path 2: Check for auth code
            val code = parsedUrl.parameters["code"]
            if (!code.isNullOrBlank()) {
                val tokenResult = AniListApi.exchangeCodeForToken(code)
                if (tokenResult != null) {
                    completeAuthWithToken(
                        token = tokenResult.accessToken,
                        refreshToken = tokenResult.refreshToken,
                        expiresInSeconds = tokenResult.expiresInSeconds
                    )
                } else {
                    publish(isLoading = false, errorMessage = "Failed to exchange authorization code.")
                }
                return@launch
            }

            publish(isLoading = false, errorMessage = "No access token or authorization code found in callback.")
        }
    }

    private suspend fun completeAuthWithToken(token: String, refreshToken: String?, expiresInSeconds: Long) {
        val viewer = AniListApi.fetchViewer(token)
        if (viewer == null) {
            publish(isLoading = false, errorMessage = "Failed to fetch user profile details from AniList.")
            return
        }

        val expiresAt = WatchProgressClock.nowEpochMs() + (expiresInSeconds * 1000L)
        authState = AniListAuthState(
            accessToken = token,
            refreshToken = refreshToken,
            username = viewer.name,
            avatarUrl = viewer.avatar?.large,
            userId = viewer.id,
            tokenExpiresAtEpochMs = expiresAt
        )
        persist()
        _isAuthenticated.value = true
        publish(isLoading = false, errorMessage = null)
        
        // Trigger initial sync and library loading after connection
        scope.launch {
            runCatching {
                AniListLibraryRepository.refreshNow()
                AniListSyncCoordinator.syncNow()
            }.onFailure {
                log.e { "Initial AniList sync after login failed: ${it.message}" }
            }
        }
    }

    private suspend fun refreshTokenIfNeeded(): Boolean {
        val state = authState
        val refreshTok = state.refreshToken
        if (refreshTok.isNullOrBlank()) return true

        val expiresAt = state.tokenExpiresAtEpochMs
        if (expiresAt != null && WatchProgressClock.nowEpochMs() < expiresAt - 60_000L) {
            return true
        }

        log.i { "AniList access token expired or expiring. Refreshing..." }
        publish(isLoading = false, errorMessage = null)

        return try {
            val result = AniListApi.refreshAccessToken(refreshTok)
            if (result != null) {
                val newExpiresAt = WatchProgressClock.nowEpochMs() + (result.expiresInSeconds * 1000L)
                authState = authState.copy(
                    accessToken = result.accessToken,
                    refreshToken = result.refreshToken,
                    tokenExpiresAtEpochMs = newExpiresAt
                )
                persist()
                publish()
                log.i { "AniList token refreshed successfully." }
                true
            } else {
                log.e { "Failed to refresh AniList token — response was null." }
                false
            }
        } catch (e: Exception) {
            log.e { "Failed to refresh AniList token: ${e.message}" }
            false
        }
    }

    fun disconnect() {
        clearLocalState()
        scope.launch {
            AniListLibraryRepository.clearLocalState()
        }
    }

    fun getAccessToken(): String? {
        ensureLoaded()
        return authState.accessToken
    }

    suspend fun getAccessTokenRefreshed(): String? {
        ensureLoaded()
        val success = refreshTokenIfNeeded()
        if (!success) return null
        return authState.accessToken
    }

    fun getUserId(): Int? {
        ensureLoaded()
        return authState.userId
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val payload = AniListStorage.loadAuthPayload().orEmpty().trim()
        authState = if (payload.isBlank()) {
            AniListAuthState()
        } else {
            runCatching { json.decodeFromString<AniListAuthState>(payload) }
                .getOrElse {
                    log.w { "Failed to parse AniList auth payload: ${it.message}" }
                    AniListAuthState()
                }
        }
        _isAuthenticated.value = authState.isAuthenticated
        publish()
    }

    private fun persist() {
        runCatching {
            val payload = json.encodeToString(authState)
            AniListStorage.saveAuthPayload(payload)
        }.onFailure {
            log.w { "Failed to persist AniList auth state: ${it.message}" }
        }
    }

    private fun publish(
        isLoading: Boolean = _uiState.value.isLoading,
        errorMessage: String? = _uiState.value.errorMessage
    ) {
        val mode = when {
            authState.isAuthenticated -> AniListConnectionMode.CONNECTED
            isLoading -> AniListConnectionMode.LOADING
            else -> AniListConnectionMode.DISCONNECTED
        }

        _uiState.value = AniListAuthUiState(
            mode = mode,
            username = authState.username,
            avatarUrl = authState.avatarUrl,
            tokenExpiresAtEpochMs = authState.tokenExpiresAtEpochMs,
            isLoading = isLoading,
            errorMessage = errorMessage
        )
    }

    private fun localizedString(resource: StringResource): String = runBlocking { getString(resource) }
}
