package com.nuvio.app.features.cloudstream

object CloudStreamCompatibilityResolver {
    private val crossPlatformAdapters = mapOf(
        "https://raw.githubusercontent.com/Kraptor123/cs-kraptor/master/repo.json#KickTR" to "kick-tr-v1",
    )

    fun resolve(
        metadata: CloudStreamPluginMetadata,
        supportsAndroidDex: Boolean = false,
    ): CloudStreamCompatibility {
        val adapterId = crossPlatformAdapters[metadata.id.value]
        return when {
            adapterId != null -> {
            CloudStreamCompatibility(
                runtimeKind = CloudStreamRuntimeKind.PrecompiledCrossPlatformAdapter,
                platformSupport = CloudStreamPlatformSupport.AndroidAndIos,
                adapterId = adapterId,
                reason = "Questo provider ha un adattatore cross-platform verificato compilato in Nuvio Enhanced.",
            )
            }
            supportsAndroidDex -> {
                CloudStreamCompatibility(
                    runtimeKind = CloudStreamRuntimeKind.AndroidDex,
                    platformSupport = CloudStreamPlatformSupport.AndroidOnly,
                    reason = "Le build Android full eseguono questo pacchetto .cs3 CloudStream standard con il runtime CloudStream integrato.",
                )
            }
            else -> {
            CloudStreamCompatibility(
                runtimeKind = CloudStreamRuntimeKind.UnsupportedAndroidDex,
                platformSupport = CloudStreamPlatformSupport.Unsupported,
                reason = "Questo pacchetto .cs3 standard contiene codice DEX Android, che non può essere eseguito su iOS.",
            )
            }
        }
    }
}

fun sortCloudStreamEpisodes(episodes: List<CloudStreamEpisode>): List<CloudStreamEpisode> =
    episodes.sortedWith(
        compareBy<CloudStreamEpisode>(
            { it.season ?: Int.MAX_VALUE },
            { it.episode ?: Int.MAX_VALUE },
            { it.name.lowercase() },
        ),
    )
