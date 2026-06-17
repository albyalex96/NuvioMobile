package com.nuvio.app.features.plugins

import com.nuvio.app.core.storage.DesktopStorage

internal object PluginStorage {
    private val store = DesktopStorage.store("nuvio_plugin_storage")

    fun loadState(profileId: Int): String? =
        store.getString("plugin_state_$profileId")

    fun saveState(profileId: Int, state: String) {
        store.putString("plugin_state_$profileId", state)
    }

    fun loadScraperSettings(scraperId: String): String? =
        store.getString("scraper_settings_$scraperId")

    fun saveScraperSettings(scraperId: String, settings: String) {
        store.putString("scraper_settings_$scraperId", settings)
    }
}

internal fun currentEpochMillis(): Long = System.currentTimeMillis()

internal fun currentPluginPlatform(): String = "desktop"
