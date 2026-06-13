package com.nuvio.app.features.plugins.cloudstream

import com.lagradost.cloudstream3.TvType

fun TvType.toNuvioType(): String = when (this) {
    TvType.Movie, TvType.AnimeMovie, TvType.Documentary, TvType.Torrent -> "movie"
    else -> "tv"
}

fun tvTypeFromString(value: String): TvType? = TvType.entries.firstOrNull {
    it.name.equals(value, ignoreCase = true)
}
