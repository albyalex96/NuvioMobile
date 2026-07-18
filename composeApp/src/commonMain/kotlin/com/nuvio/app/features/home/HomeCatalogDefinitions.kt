package com.nuvio.app.features.home

import com.nuvio.app.core.i18n.localizedMediaTypeLabel
import com.nuvio.app.features.addons.AddonCatalog
import com.nuvio.app.features.addons.AddonManifest
import com.nuvio.app.features.addons.ManagedAddon
import com.nuvio.app.features.addons.enabledAddons
import com.nuvio.app.features.anilist.AniListLibraryRepository
import com.nuvio.app.features.catalog.supportsPagination
import kotlinx.coroutines.runBlocking
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.home_catalog_default_title
import org.jetbrains.compose.resources.getString

data class HomeCatalogDefinition(
    val key: String,
    val defaultTitle: String,
    val catalogName: String,
    val addonName: String,
    val manifestUrl: String,
    val type: String,
    val catalogId: String,
    val supportsPagination: Boolean,
    val descriptorSignature: String,
) {
    val cacheKey: String
        get() = "$key|$descriptorSignature"

    fun titleFor(showCatalogType: Boolean): String =
        if (showCatalogType) defaultTitle else catalogName
}

fun buildHomeCatalogRefreshSignature(addons: List<ManagedAddon>): List<String> =
    addons.enabledAddons().mapNotNull { addon ->
        val manifest = addon.manifest ?: return@mapNotNull null
        addon to manifest
    }.flatMap { (addon, manifest) ->
        manifest.catalogs.map { catalog ->
            buildHomeCatalogDescriptorSignature(addon, manifest, catalog)
        }
    }.sorted()

fun buildHomeCatalogDefinitions(addons: List<ManagedAddon>): List<HomeCatalogDefinition> =
    addons.enabledAddons().mapNotNull { addon ->
        val manifest = addon.manifest ?: return@mapNotNull null
        addon to manifest
    }.flatMap { (addon, manifest) ->
        manifest.catalogs
            .filter { catalog -> catalog.extra.none { it.isRequired } }
            .map { catalog ->
                HomeCatalogDefinition(
                    key = "${manifest.id}:${catalog.type}:${catalog.id}",
                    defaultTitle = runBlocking {
                        getString(
                            Res.string.home_catalog_default_title,
                            catalog.name,
                            localizedMediaTypeLabel(catalog.type),
                        )
                    },
                    catalogName = catalog.name,
                    addonName = addon.displayTitle,
                    manifestUrl = addon.manifestUrl,
                    type = catalog.type,
                    catalogId = catalog.id,
                    supportsPagination = catalog.supportsPagination(),
                    descriptorSignature = buildHomeCatalogDescriptorSignature(addon, manifest, catalog),
                )
            }
    }.distinctBy(HomeCatalogDefinition::key)

private fun buildHomeCatalogDescriptorSignature(
    addon: ManagedAddon,
    manifest: AddonManifest,
    catalog: AddonCatalog,
): String =
    buildString {
        append(addon.displayTitle)
        append('|')
        append(addon.enabled)
        append('|')
        append(addon.isRefreshing)
        append('|')
        append(addon.errorMessage.orEmpty())
        append('|')
        append(addon.manifestUrl)
        append('|')
        append(manifest.id)
        append('|')
        append(manifest.name)
        append('|')
        append(manifest.version)
        append('|')
        append(manifest.description)
        append('|')
        append(manifest.logoUrl.orEmpty())
        append('|')
        append(manifest.transportUrl)
        append('|')
        append(manifest.types.joinToString(","))
        append('|')
        append(manifest.idPrefixes.joinToString(","))
        append('|')
        append(manifest.resources.joinToString(",") { resource ->
            listOf(
                resource.name,
                resource.types.joinToString("/"),
                resource.idPrefixes.joinToString("/"),
            ).joinToString(":")
        })
        append('|')
        append(
            listOf(
                manifest.behaviorHints.configurable,
                manifest.behaviorHints.configurationRequired,
                manifest.behaviorHints.adult,
                manifest.behaviorHints.p2p,
            ).joinToString(":"),
        )
        append('|')
        append(catalog.type)
        append('|')
        append(catalog.id)
        append('|')
        append(catalog.name)
        append('|')
        append(catalog.supportsPagination())
        append('|')
        append(catalog.extra.joinToString(",") { extra ->
            listOf(
                extra.name,
                extra.isRequired.toString(),
                extra.options.joinToString("/"),
                extra.optionsLimit?.toString().orEmpty(),
            ).joinToString(":")
        })
    }

internal fun String.displayLabel(): String = localizedMediaTypeLabel(this)

private val aniListGroupLabels: Map<String, String> = mapOf(
    "watching" to "Currently Watching",
    "rewatching" to "Rewatching",
    "completed" to "Completed",
    "planning" to "Plan to Watch",
    "paused" to "Paused",
    "dropped" to "Dropped",
)

fun buildAniListDefinitions(isAuthenticated: Boolean): List<HomeCatalogDefinition> {
    if (!isAuthenticated) return emptyList()
    return aniListGroupLabels.map { (key, label) ->
        HomeCatalogDefinition(
            key = "anilist_$key",
            defaultTitle = label,
            catalogName = label,
            addonName = "AniList",
            manifestUrl = "",
            type = "anime-series",
            catalogId = key,
            supportsPagination = false,
            descriptorSignature = "anilist|$key",
        )
    }
}
