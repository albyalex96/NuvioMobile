package com.nuvio.app.features.plugins

import kotlinx.serialization.json.Json
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.plugins_manifest_name_missing
import nuvio.composeapp.generated.resources.plugins_manifest_no_providers
import nuvio.composeapp.generated.resources.plugins_manifest_version_missing
import org.jetbrains.compose.resources.getString

internal object PluginManifestParser {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun parse(payload: String): PluginManifest {
        val manifest = json.decodeFromString<PluginManifest>(payload)
        val nameMissing = getString(Res.string.plugins_manifest_name_missing)
        require(manifest.name.isNotBlank()) { nameMissing }
        val versionMissing = getString(Res.string.plugins_manifest_version_missing)
        require(manifest.version.isNotBlank()) { versionMissing }
        val noProviders = getString(Res.string.plugins_manifest_no_providers)
        require(manifest.scrapers.isNotEmpty()) { noProviders }
        return manifest
    }
}
