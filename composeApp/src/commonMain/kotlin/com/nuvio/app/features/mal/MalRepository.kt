package com.nuvio.app.features.mal

object MalRepository {
    private val apiClient get() = MalApiClient
    private val authRepository get() = MalAuthRepository

    fun isAuthenticated(): Boolean {
        authRepository.ensureLoaded()
        return authRepository.snapshot().mode == MalConnectionMode.CONNECTED
    }

    fun getClientId(): String = MalConfig.CLIENT_ID

    suspend fun searchAnime(
        query: String,
        limit: Int = 10,
        offset: Int = 0,
    ): MalSearchResponse {
        return apiClient.searchAnime(
            clientId = MalConfig.CLIENT_ID,
            query = query,
            limit = limit,
            offset = offset,
        )
    }

    suspend fun getAnimeDetails(animeId: Long): MalAnime {
        return apiClient.getAnimeDetails(
            clientId = MalConfig.CLIENT_ID,
            animeId = animeId,
        )
    }

    suspend fun getRanking(
        rankingType: String = "all",
        limit: Int = 10,
        offset: Int = 0,
    ): MalSearchResponse {
        return apiClient.getRanking(
            clientId = MalConfig.CLIENT_ID,
            rankingType = rankingType,
            limit = limit,
            offset = offset,
        )
    }
}
