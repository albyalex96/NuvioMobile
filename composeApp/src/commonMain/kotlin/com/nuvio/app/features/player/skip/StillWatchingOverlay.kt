package com.nuvio.app.features.player.skip

internal fun shouldEnterStillWatchingPrompt(
    stillWatchingEnabled: Boolean,
    autoPlayNextEpisodeEnabled: Boolean,
    nextEpisodeHasAired: Boolean,
    consecutiveAutoPlayCount: Int,
    threshold: Int,
): Boolean {
    if (!stillWatchingEnabled) return false
    if (!autoPlayNextEpisodeEnabled) return false
    if (!nextEpisodeHasAired) return false
    return consecutiveAutoPlayCount > 0 && consecutiveAutoPlayCount % threshold == 0
}