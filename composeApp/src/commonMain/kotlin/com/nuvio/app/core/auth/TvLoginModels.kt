package com.nuvio.app.core.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TvLoginStartResult(
    val code: String,
    @SerialName("web_url") val webUrl: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("poll_interval_seconds") val pollIntervalSeconds: Int = 3,
)

@Serializable
data class TvLoginPollResult(
    val status: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("poll_interval_seconds") val pollIntervalSeconds: Int? = null,
)

@Serializable
data class TvLoginExchangeResult(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)
