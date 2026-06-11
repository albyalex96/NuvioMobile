package com.nuvio.app.features.plugins

import com.nuvio.app.core.platform.WebKeyValueStorage

internal object PluginStorage {
    private const val namespace = "nuvio_plugins"

    fun loadState(profileId: Int): String? =
        WebKeyValueStorage.getString(namespace, "state_$profileId")

    fun saveState(profileId: Int, payload: String) {
        WebKeyValueStorage.setString(namespace, "state_$profileId", payload)
    }

    fun loadScraperSettings(scraperId: String): String? =
        WebKeyValueStorage.getString(namespace, "settings_$scraperId")

    fun saveScraperSettings(scraperId: String, payload: String) {
        WebKeyValueStorage.setString(namespace, "settings_$scraperId", payload)
    }
}

internal fun currentPluginPlatform(): String = "web"

@JsFun("() => Date.now()")
private external fun jsNow(): Double

internal fun currentEpochMillis(): Long = jsNow().toLong()
