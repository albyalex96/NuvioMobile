package com.nuvio.app.core.auth

import co.touchlab.kermit.Logger
import com.nuvio.app.core.network.SupabaseProvider
import com.nuvio.app.core.storage.LocalAccountDataCleaner
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.statement.bodyAsText
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString

object AuthRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val log = Logger.withTag("AuthRepository")

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var initialized = false

    fun initialize() {
        if (initialized) return
        initialized = true

        val savedAnonId = AuthStorage.loadAnonymousUserId()
        if (savedAnonId != null) {
            _state.value = AuthState.Authenticated(
                userId = savedAnonId,
                email = null,
                isAnonymous = true,
            )
        }

        scope.launch {
            SupabaseProvider.client.auth.sessionStatus.collect { status ->
                if (AuthStorage.loadAnonymousUserId() != null) return@collect
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val user = status.session.user
                        if (user?.email.isNullOrBlank()) return@collect
                        _state.value = AuthState.Authenticated(
                            userId = user?.id ?: "",
                            email = user?.email,
                            isAnonymous = false,
                        )
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _state.value = AuthState.Unauthenticated
                    }
                    is SessionStatus.Initializing -> {
                        if (savedAnonId == null) _state.value = AuthState.Loading
                    }
                    is SessionStatus.RefreshFailure -> {
                        _state.value = AuthState.Unauthenticated
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun signInAnonymously() {
        _error.value = null
        val userId = Uuid.random().toString()
        AuthStorage.saveAnonymousUserId(userId)
        _state.value = AuthState.Authenticated(
            userId = userId,
            email = null,
            isAnonymous = true,
        )
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> = runCatching {
        _error.value = null
        SupabaseProvider.client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        Unit
    }.onFailure { e ->
        log.e(e) { "Email sign-up failed" }
        _error.value = e.message ?: getString(Res.string.auth_sign_up_failed)
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> = runCatching {
        _error.value = null
        SupabaseProvider.client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }.onFailure { e ->
        log.e(e) { "Email sign-in failed" }
        _error.value = e.message ?: getString(Res.string.auth_sign_in_failed)
    }

    suspend fun signOut(): Result<Unit> = runCatching {
        _error.value = null
        val wasAnonymous = AuthStorage.loadAnonymousUserId() != null
        AuthStorage.clearAnonymousUserId()
        if (!wasAnonymous) {
            SupabaseProvider.client.auth.signOut()
        }
        _state.value = AuthState.Unauthenticated
        LocalAccountDataCleaner.wipe()
    }.onFailure { e ->
        log.e(e) { "Sign-out failed" }
        _error.value = e.message ?: getString(Res.string.auth_sign_out_failed)
    }

    suspend fun deleteAccount(): Result<Unit> = runCatching {
        _error.value = null
        SupabaseProvider.client.functions.invoke("delete-account")
        SupabaseProvider.client.auth.signOut()
        LocalAccountDataCleaner.wipe()
    }.onFailure { e ->
        log.e(e) { "Account deletion failed" }
        _error.value = e.message ?: getString(Res.string.auth_account_deletion_failed)
    }

    fun clearError() {
        _error.value = null
    }

    fun generateDeviceNonce(): String {
        val bytes = ByteArray(24)
        kotlin.random.Random.nextBytes(bytes)
        return bytes.joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
    }

    suspend fun ensureQrSessionAuthenticated(): Result<Unit> = runCatching {
        val user = SupabaseProvider.client.auth.currentUserOrNull()
        val hasToken = SupabaseProvider.client.auth.currentAccessTokenOrNull() != null
        if (user != null && hasToken) return@runCatching
        SupabaseProvider.client.auth.signInAnonymously()
    }.onFailure { e ->
        log.e(e) { "QR anonymous sign-in failed" }
    }

    suspend fun startTvLoginSession(
        deviceNonce: String,
        deviceName: String?,
        redirectBaseUrl: String,
    ): Result<TvLoginStartResult> = runCatching {
        val params = buildJsonObject {
            put("p_device_nonce", deviceNonce)
            put("p_redirect_base_url", redirectBaseUrl)
            if (!deviceName.isNullOrBlank()) put("p_device_name", deviceName)
        }
        val response = SupabaseProvider.client.postgrest.rpc("start_tv_login_session", params)
        response.decodeList<TvLoginStartResult>().firstOrNull()
            ?: throw Exception("Empty response from start_tv_login_session")
    }.onFailure { e ->
        log.e(e) { "Failed to start TV login session" }
    }

    suspend fun pollTvLoginSession(code: String, deviceNonce: String): Result<TvLoginPollResult> = runCatching {
        val params = buildJsonObject {
            put("p_code", code)
            put("p_device_nonce", deviceNonce)
        }
        val response = SupabaseProvider.client.postgrest.rpc("poll_tv_login_session", params)
        response.decodeList<TvLoginPollResult>().firstOrNull()
            ?: throw Exception("Empty response from poll_tv_login_session")
    }.onFailure { e ->
        log.e(e) { "Failed to poll TV login session" }
    }

    suspend fun exchangeTvLoginSession(code: String, deviceNonce: String): Result<Unit> = runCatching {
        val token = SupabaseProvider.client.auth.currentAccessTokenOrNull()
            ?: throw Exception("Not authenticated")
        val payload = buildJsonObject {
            put("code", code)
            put("device_nonce", deviceNonce)
        }
        val httpResponse = SupabaseProvider.client.functions.invoke(
            function = "tv-logins-exchange",
            body = payload.toString(),
        )
        val result = Json.decodeFromString<TvLoginExchangeResult>(httpResponse.bodyAsText())
        SupabaseProvider.client.auth.importAuthToken(result.accessToken, result.refreshToken)
    }.onFailure { e ->
        log.e(e) { "Failed to exchange TV login session" }
    }
}
