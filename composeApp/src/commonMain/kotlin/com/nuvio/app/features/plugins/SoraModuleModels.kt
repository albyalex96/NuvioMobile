package com.nuvio.app.features.plugins

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SoraModuleJson(
    @SerialName("sourceName") val sourceName: String,
    @SerialName("iconUrl") val iconUrl: String? = null,
    val author: SoraModuleAuthor? = null,
    val version: String,
    val language: String? = null,
    @SerialName("streamType") val streamType: String? = null,
    val quality: String? = null,
    @SerialName("baseUrl") val baseUrl: String? = null,
    @SerialName("searchBaseUrl") val searchBaseUrl: String? = null,
    @SerialName("scriptUrl") val scriptUrl: String,
    @SerialName("asyncJS") val asyncJS: Boolean? = null,
    @SerialName("streamAsyncJS") val streamAsyncJS: Boolean? = null,
    val softsub: Boolean? = null,
    val type: String? = null,
    @SerialName("downloadSupport") val downloadSupport: Boolean? = null,
)

@Serializable
data class SoraModuleAuthor(
    val name: String,
    val icon: String? = null,
    val url: String? = null,
)

data class SoraModuleItem(
    val moduleUrl: String,
    val sourceName: String,
    val iconUrl: String? = null,
    val authorName: String? = null,
    val version: String,
    val language: String? = null,
    val streamType: String? = null,
    val quality: String? = null,
    val baseUrl: String? = null,
    val searchBaseUrl: String? = null,
    val scriptUrl: String,
    val asyncJS: Boolean = false,
    val type: String? = null,
    val downloadSupport: Boolean = false,
    val softsub: Boolean = false,
    val enabled: Boolean = true,
    val jsCode: String = "",
)

@Serializable
internal data class StoredSoraModule(
    val moduleUrl: String,
    val sourceName: String,
    val iconUrl: String? = null,
    val authorName: String? = null,
    val version: String,
    val language: String? = null,
    val streamType: String? = null,
    val quality: String? = null,
    val baseUrl: String? = null,
    val searchBaseUrl: String? = null,
    val scriptUrl: String,
    val asyncJS: Boolean = false,
    val type: String? = null,
    val downloadSupport: Boolean = false,
    val softsub: Boolean = false,
    val enabled: Boolean = true,
    val jsCode: String = "",
)
