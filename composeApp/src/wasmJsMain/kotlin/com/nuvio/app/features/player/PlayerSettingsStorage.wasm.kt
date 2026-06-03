package com.nuvio.app.features.player

import com.nuvio.app.WebStorage
import kotlinx.serialization.json.*

internal actual object PlayerSettingsStorage {
    private const val KEY = "nuvio_player_settings"

    private fun prefs(): JsonObject {
        val raw = WebStorage.getString(KEY) ?: return JsonObject(emptyMap())
        return Json.parseToJsonElement(raw).jsonObject
    }

    private fun save(obj: JsonObject) {
        WebStorage.setString(KEY, Json.encodeToString(JsonObject.serializer(), obj))
    }

    private inline fun <reified T> saveField(name: String, value: T) {
        val p = prefs().toMutableMap()
        p[name] = when (value) {
            is Boolean -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Float -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            null -> JsonPrimitive(null)
            else -> JsonPrimitive(value.toString())
        }
        save(JsonObject(p))
    }

    actual fun loadShowLoadingOverlay(): Boolean? = prefs()["showLoadingOverlay"]?.jsonPrimitive?.booleanOrNull
    actual fun saveShowLoadingOverlay(enabled: Boolean) { saveField("showLoadingOverlay", enabled) }
    actual fun loadResizeMode(): String? = prefs()["resizeMode"]?.jsonPrimitive?.contentOrNull
    actual fun saveResizeMode(mode: String) { saveField("resizeMode", mode) }
    actual fun loadHoldToSpeedEnabled(): Boolean? = prefs()["holdToSpeedEnabled"]?.jsonPrimitive?.booleanOrNull
    actual fun saveHoldToSpeedEnabled(enabled: Boolean) { saveField("holdToSpeedEnabled", enabled) }
    actual fun loadHoldToSpeedValue(): Float? = prefs()["holdToSpeedValue"]?.jsonPrimitive?.floatOrNull
    actual fun saveHoldToSpeedValue(speed: Float) { saveField("holdToSpeedValue", speed) }
    actual fun loadExternalPlayerEnabled(): Boolean? = prefs()["externalPlayerEnabled"]?.jsonPrimitive?.booleanOrNull
    actual fun saveExternalPlayerEnabled(enabled: Boolean) { saveField("externalPlayerEnabled", enabled) }
    actual fun loadExternalPlayerForwardSubtitles(): Boolean? = prefs()["externalPlayerForwardSubtitles"]?.jsonPrimitive?.booleanOrNull
    actual fun saveExternalPlayerForwardSubtitles(enabled: Boolean) { saveField("externalPlayerForwardSubtitles", enabled) }
    actual fun loadExternalPlayerId(): String? = prefs()["externalPlayerId"]?.jsonPrimitive?.contentOrNull
    actual fun saveExternalPlayerId(playerId: String?) { saveField("externalPlayerId", playerId) }
    actual fun loadPreferredAudioLanguage(): String? = prefs()["preferredAudioLanguage"]?.jsonPrimitive?.contentOrNull
    actual fun savePreferredAudioLanguage(language: String) { saveField("preferredAudioLanguage", language) }
    actual fun loadSecondaryPreferredAudioLanguage(): String? = prefs()["secondaryPreferredAudioLanguage"]?.jsonPrimitive?.contentOrNull
    actual fun saveSecondaryPreferredAudioLanguage(language: String?) { saveField("secondaryPreferredAudioLanguage", language) }
    actual fun loadPreferredSubtitleLanguage(): String? = prefs()["preferredSubtitleLanguage"]?.jsonPrimitive?.contentOrNull
    actual fun savePreferredSubtitleLanguage(language: String) { saveField("preferredSubtitleLanguage", language) }
    actual fun loadSecondaryPreferredSubtitleLanguage(): String? = prefs()["secondaryPreferredSubtitleLanguage"]?.jsonPrimitive?.contentOrNull
    actual fun saveSecondaryPreferredSubtitleLanguage(language: String?) { saveField("secondaryPreferredSubtitleLanguage", language) }
    actual fun loadSubtitleTextColor(): String? = prefs()["subtitleTextColor"]?.jsonPrimitive?.contentOrNull
    actual fun saveSubtitleTextColor(colorHex: String) { saveField("subtitleTextColor", colorHex) }
    actual fun loadSubtitleBackgroundColor(): String? = prefs()["subtitleBackgroundColor"]?.jsonPrimitive?.contentOrNull
    actual fun saveSubtitleBackgroundColor(colorHex: String) { saveField("subtitleBackgroundColor", colorHex) }
    actual fun loadSubtitleOutlineColor(): String? = prefs()["subtitleOutlineColor"]?.jsonPrimitive?.contentOrNull
    actual fun saveSubtitleOutlineColor(colorHex: String) { saveField("subtitleOutlineColor", colorHex) }
    actual fun loadSubtitleOutlineEnabled(): Boolean? = prefs()["subtitleOutlineEnabled"]?.jsonPrimitive?.booleanOrNull
    actual fun saveSubtitleOutlineEnabled(enabled: Boolean) { saveField("subtitleOutlineEnabled", enabled) }
    actual fun loadSubtitleOutlineWidth(): Int? = prefs()["subtitleOutlineWidth"]?.jsonPrimitive?.intOrNull
    actual fun saveSubtitleOutlineWidth(width: Int) { saveField("subtitleOutlineWidth", width) }
    actual fun loadSubtitleBold(): Boolean? = prefs()["subtitleBold"]?.jsonPrimitive?.booleanOrNull
    actual fun saveSubtitleBold(enabled: Boolean) { saveField("subtitleBold", enabled) }
    actual fun loadSubtitleFontSizeSp(): Int? = prefs()["subtitleFontSizeSp"]?.jsonPrimitive?.intOrNull
    actual fun saveSubtitleFontSizeSp(fontSizeSp: Int) { saveField("subtitleFontSizeSp", fontSizeSp) }
    actual fun loadSubtitleBottomOffset(): Int? = prefs()["subtitleBottomOffset"]?.jsonPrimitive?.intOrNull
    actual fun saveSubtitleBottomOffset(bottomOffset: Int) { saveField("subtitleBottomOffset", bottomOffset) }
    actual fun loadSubtitleUseForcedSubtitles(): Boolean? = prefs()["subtitleUseForcedSubtitles"]?.jsonPrimitive?.booleanOrNull
    actual fun saveSubtitleUseForcedSubtitles(enabled: Boolean) { saveField("subtitleUseForcedSubtitles", enabled) }
    actual fun loadSubtitleShowOnlyPreferredLanguages(): Boolean? = prefs()["subtitleShowOnlyPreferredLanguages"]?.jsonPrimitive?.booleanOrNull
    actual fun saveSubtitleShowOnlyPreferredLanguages(enabled: Boolean) { saveField("subtitleShowOnlyPreferredLanguages", enabled) }
    actual fun loadAddonSubtitleStartupMode(): String? = prefs()["addonSubtitleStartupMode"]?.jsonPrimitive?.contentOrNull
    actual fun saveAddonSubtitleStartupMode(mode: String) { saveField("addonSubtitleStartupMode", mode) }
    actual fun loadStreamReuseLastLinkEnabled(): Boolean? = prefs()["streamReuseLastLinkEnabled"]?.jsonPrimitive?.booleanOrNull
    actual fun saveStreamReuseLastLinkEnabled(enabled: Boolean) { saveField("streamReuseLastLinkEnabled", enabled) }
    actual fun loadStreamReuseLastLinkCacheHours(): Int? = prefs()["streamReuseLastLinkCacheHours"]?.jsonPrimitive?.intOrNull
    actual fun saveStreamReuseLastLinkCacheHours(hours: Int) { saveField("streamReuseLastLinkCacheHours", hours) }
    actual fun loadDecoderPriority(): Int? = prefs()["decoderPriority"]?.jsonPrimitive?.intOrNull
    actual fun saveDecoderPriority(priority: Int) { saveField("decoderPriority", priority) }
    actual fun loadMapDV7ToHevc(): Boolean? = prefs()["mapDV7ToHevc"]?.jsonPrimitive?.booleanOrNull
    actual fun saveMapDV7ToHevc(enabled: Boolean) { saveField("mapDV7ToHevc", enabled) }
    actual fun loadTunnelingEnabled(): Boolean? = prefs()["tunnelingEnabled"]?.jsonPrimitive?.booleanOrNull
    actual fun saveTunnelingEnabled(enabled: Boolean) { saveField("tunnelingEnabled", enabled) }
    actual fun loadStreamAutoPlayMode(): String? = prefs()["streamAutoPlayMode"]?.jsonPrimitive?.contentOrNull
    actual fun saveStreamAutoPlayMode(mode: String) { saveField("streamAutoPlayMode", mode) }
    actual fun loadStreamAutoPlaySource(): String? = prefs()["streamAutoPlaySource"]?.jsonPrimitive?.contentOrNull
    actual fun saveStreamAutoPlaySource(source: String) { saveField("streamAutoPlaySource", source) }
    actual fun loadStreamAutoPlaySelectedAddons(): Set<String>? = null
    actual fun saveStreamAutoPlaySelectedAddons(addons: Set<String>) {}
    actual fun loadStreamAutoPlaySelectedPlugins(): Set<String>? = null
    actual fun saveStreamAutoPlaySelectedPlugins(plugins: Set<String>) {}
    actual fun loadStreamAutoPlayRegex(): String? = prefs()["streamAutoPlayRegex"]?.jsonPrimitive?.contentOrNull
    actual fun saveStreamAutoPlayRegex(regex: String) { saveField("streamAutoPlayRegex", regex) }
    actual fun loadStreamAutoPlayTimeoutSeconds(): Int? = prefs()["streamAutoPlayTimeoutSeconds"]?.jsonPrimitive?.intOrNull
    actual fun saveStreamAutoPlayTimeoutSeconds(seconds: Int) { saveField("streamAutoPlayTimeoutSeconds", seconds) }
    actual fun loadSkipIntroEnabled(): Boolean? = prefs()["skipIntroEnabled"]?.jsonPrimitive?.booleanOrNull
    actual fun saveSkipIntroEnabled(enabled: Boolean) { saveField("skipIntroEnabled", enabled) }
    actual fun loadAnimeSkipEnabled(): Boolean? = prefs()["animeSkipEnabled"]?.jsonPrimitive?.booleanOrNull
    actual fun saveAnimeSkipEnabled(enabled: Boolean) { saveField("animeSkipEnabled", enabled) }
    actual fun loadAnimeSkipClientId(): String? = prefs()["animeSkipClientId"]?.jsonPrimitive?.contentOrNull
    actual fun saveAnimeSkipClientId(clientId: String) { saveField("animeSkipClientId", clientId) }
    actual fun loadIntroDbApiKey(): String? = prefs()["introDbApiKey"]?.jsonPrimitive?.contentOrNull
    actual fun saveIntroDbApiKey(apiKey: String) { saveField("introDbApiKey", apiKey) }
    actual fun loadIntroSubmitEnabled(): Boolean? = prefs()["introSubmitEnabled"]?.jsonPrimitive?.booleanOrNull
    actual fun saveIntroSubmitEnabled(enabled: Boolean) { saveField("introSubmitEnabled", enabled) }
    actual fun loadStreamAutoPlayNextEpisodeEnabled(): Boolean? = prefs()["streamAutoPlayNextEpisodeEnabled"]?.jsonPrimitive?.booleanOrNull
    actual fun saveStreamAutoPlayNextEpisodeEnabled(enabled: Boolean) { saveField("streamAutoPlayNextEpisodeEnabled", enabled) }
    actual fun loadStreamAutoPlayPreferBingeGroup(): Boolean? = prefs()["streamAutoPlayPreferBingeGroup"]?.jsonPrimitive?.booleanOrNull
    actual fun saveStreamAutoPlayPreferBingeGroup(enabled: Boolean) { saveField("streamAutoPlayPreferBingeGroup", enabled) }
    actual fun loadStreamAutoPlayReuseBingeGroup(): Boolean? = prefs()["streamAutoPlayReuseBingeGroup"]?.jsonPrimitive?.booleanOrNull
    actual fun saveStreamAutoPlayReuseBingeGroup(enabled: Boolean) { saveField("streamAutoPlayReuseBingeGroup", enabled) }
    actual fun loadNextEpisodeThresholdMode(): String? = prefs()["nextEpisodeThresholdMode"]?.jsonPrimitive?.contentOrNull
    actual fun saveNextEpisodeThresholdMode(mode: String) { saveField("nextEpisodeThresholdMode", mode) }
    actual fun loadNextEpisodeThresholdPercent(): Float? = prefs()["nextEpisodeThresholdPercent"]?.jsonPrimitive?.floatOrNull
    actual fun saveNextEpisodeThresholdPercent(percent: Float) { saveField("nextEpisodeThresholdPercent", percent) }
    actual fun loadNextEpisodeThresholdMinutesBeforeEnd(): Float? = prefs()["nextEpisodeThresholdMinutesBeforeEnd"]?.jsonPrimitive?.floatOrNull
    actual fun saveNextEpisodeThresholdMinutesBeforeEnd(minutes: Float) { saveField("nextEpisodeThresholdMinutesBeforeEnd", minutes) }
    actual fun loadUseLibass(): Boolean? = prefs()["useLibass"]?.jsonPrimitive?.booleanOrNull
    actual fun saveUseLibass(enabled: Boolean) { saveField("useLibass", enabled) }
    actual fun loadLibassRenderType(): String? = prefs()["libassRenderType"]?.jsonPrimitive?.contentOrNull
    actual fun saveLibassRenderType(renderType: String) { saveField("libassRenderType", renderType) }
    actual fun loadEpisodeCodeFormat(): String? = prefs()["episodeCodeFormat"]?.jsonPrimitive?.contentOrNull
    actual fun saveEpisodeCodeFormat(format: String) { saveField("episodeCodeFormat", format) }
    actual fun loadStillWatchingEnabled(): Boolean? = prefs()["stillWatchingEnabled"]?.jsonPrimitive?.booleanOrNull
    actual fun saveStillWatchingEnabled(enabled: Boolean) { saveField("stillWatchingEnabled", enabled) }
    actual fun loadStillWatchingEpisodeCount(): Int? = prefs()["stillWatchingEpisodeCount"]?.jsonPrimitive?.intOrNull
    actual fun saveStillWatchingEpisodeCount(count: Int) { saveField("stillWatchingEpisodeCount", count) }
    actual fun loadStillWatchingNightMode(): Boolean? = prefs()["stillWatchingNightMode"]?.jsonPrimitive?.booleanOrNull
    actual fun saveStillWatchingNightMode(enabled: Boolean) { saveField("stillWatchingNightMode", enabled) }
    actual fun loadSwipeGesturesEnabled(): Boolean? = prefs()["swipeGesturesEnabled"]?.jsonPrimitive?.booleanOrNull
    actual fun saveSwipeGesturesEnabled(enabled: Boolean) { saveField("swipeGesturesEnabled", enabled) }
    actual fun loadIosVideoOutputPreset(): String? = prefs()["iosVideoOutputPreset"]?.jsonPrimitive?.contentOrNull
    actual fun saveIosVideoOutputPreset(preset: String) { saveField("iosVideoOutputPreset", preset) }
    actual fun loadIosToneMappingMode(): String? = prefs()["iosToneMappingMode"]?.jsonPrimitive?.contentOrNull
    actual fun saveIosToneMappingMode(mode: String) { saveField("iosToneMappingMode", mode) }
    actual fun loadIosTargetPrimaries(): String? = prefs()["iosTargetPrimaries"]?.jsonPrimitive?.contentOrNull
    actual fun saveIosTargetPrimaries(primaries: String) { saveField("iosTargetPrimaries", primaries) }
    actual fun loadIosTargetTransfer(): String? = prefs()["iosTargetTransfer"]?.jsonPrimitive?.contentOrNull
    actual fun saveIosTargetTransfer(transfer: String) { saveField("iosTargetTransfer", transfer) }
    actual fun loadIosHardwareDecoderMode(): String? = prefs()["iosHardwareDecoderMode"]?.jsonPrimitive?.contentOrNull
    actual fun saveIosHardwareDecoderMode(mode: String) { saveField("iosHardwareDecoderMode", mode) }
    actual fun loadIosExtendedDynamicRangeEnabled(): Boolean? = prefs()["iosExtendedDynamicRangeEnabled"]?.jsonPrimitive?.booleanOrNull
    actual fun saveIosExtendedDynamicRangeEnabled(enabled: Boolean) { saveField("iosExtendedDynamicRangeEnabled", enabled) }
    actual fun loadIosTargetColorspaceHintEnabled(): Boolean? = prefs()["iosTargetColorspaceHintEnabled"]?.jsonPrimitive?.booleanOrNull
    actual fun saveIosTargetColorspaceHintEnabled(enabled: Boolean) { saveField("iosTargetColorspaceHintEnabled", enabled) }
    actual fun loadIosHdrComputePeakEnabled(): Boolean? = prefs()["iosHdrComputePeakEnabled"]?.jsonPrimitive?.booleanOrNull
    actual fun saveIosHdrComputePeakEnabled(enabled: Boolean) { saveField("iosHdrComputePeakEnabled", enabled) }
    actual fun loadIosDebandEnabled(): Boolean? = prefs()["iosDebandEnabled"]?.jsonPrimitive?.booleanOrNull
    actual fun saveIosDebandEnabled(enabled: Boolean) { saveField("iosDebandEnabled", enabled) }
    actual fun loadIosInterpolationEnabled(): Boolean? = prefs()["iosInterpolationEnabled"]?.jsonPrimitive?.booleanOrNull
    actual fun saveIosInterpolationEnabled(enabled: Boolean) { saveField("iosInterpolationEnabled", enabled) }
    actual fun loadIosBrightness(): Int? = prefs()["iosBrightness"]?.jsonPrimitive?.intOrNull
    actual fun saveIosBrightness(value: Int) { saveField("iosBrightness", value) }
    actual fun loadIosContrast(): Int? = prefs()["iosContrast"]?.jsonPrimitive?.intOrNull
    actual fun saveIosContrast(value: Int) { saveField("iosContrast", value) }
    actual fun loadIosSaturation(): Int? = prefs()["iosSaturation"]?.jsonPrimitive?.intOrNull
    actual fun saveIosSaturation(value: Int) { saveField("iosSaturation", value) }
    actual fun loadIosGamma(): Int? = prefs()["iosGamma"]?.jsonPrimitive?.intOrNull
    actual fun saveIosGamma(value: Int) { saveField("iosGamma", value) }

    actual fun exportToSyncPayload(): JsonObject = prefs()
    actual fun replaceFromSyncPayload(payload: JsonObject) {
        WebStorage.setString(KEY, Json.encodeToString(JsonObject.serializer(), payload))
    }
}
