package com.nuvio.app.features.player

import com.nuvio.app.core.platform.WebKeyValueStorage
import com.nuvio.app.core.storage.ProfileScopedKey
import com.nuvio.app.core.sync.decodeSyncBoolean
import com.nuvio.app.core.sync.decodeSyncFloat
import com.nuvio.app.core.sync.decodeSyncInt
import com.nuvio.app.core.sync.decodeSyncString
import com.nuvio.app.core.sync.decodeSyncStringSet
import com.nuvio.app.core.sync.encodeSyncBoolean
import com.nuvio.app.core.sync.encodeSyncFloat
import com.nuvio.app.core.sync.encodeSyncInt
import com.nuvio.app.core.sync.encodeSyncString
import com.nuvio.app.core.sync.encodeSyncStringSet
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal actual object PlayerSettingsStorage {
    private const val namespace = "nuvio_player_settings"
    private val allKeys: MutableList<String> = mutableListOf()
    private fun key(name: String): String { allKeys.add(name); return name }

    private val showLoadingOverlayKey = key("show_loading_overlay")
    private val resizeModeKey = key("resize_mode")
    private val holdToSpeedEnabledKey = key("hold_to_speed_enabled")
    private val holdToSpeedValueKey = key("hold_to_speed_value")
    private val externalPlayerEnabledKey = key("external_player_enabled")
    private val externalPlayerForwardSubtitlesKey = key("external_player_forward_subtitles")
    private val externalPlayerIdKey = key("external_player_id")
    private val preferredAudioLanguageKey = key("preferred_audio_language")
    private val secondaryPreferredAudioLanguageKey = key("secondary_preferred_audio_language")
    private val preferredSubtitleLanguageKey = key("preferred_subtitle_language")
    private val secondaryPreferredSubtitleLanguageKey = key("secondary_preferred_subtitle_language")
    private val subtitleTextColorKey = key("subtitle_text_color")
    private val subtitleBackgroundColorKey = key("subtitle_background_color")
    private val subtitleOutlineColorKey = key("subtitle_outline_color")
    private val subtitleOutlineEnabledKey = key("subtitle_outline_enabled")
    private val subtitleOutlineWidthKey = key("subtitle_outline_width")
    private val subtitleBoldKey = key("subtitle_bold")
    private val subtitleFontSizeSpKey = key("subtitle_font_size_sp")
    private val subtitleBottomOffsetKey = key("subtitle_bottom_offset")
    private val subtitleUseForcedSubtitlesKey = key("subtitle_use_forced_subtitles")
    private val subtitleShowOnlyPreferredLanguagesKey = key("subtitle_show_only_preferred_languages")
    private val addonSubtitleStartupModeKey = key("addon_subtitle_startup_mode")
    private val streamReuseLastLinkEnabledKey = key("stream_reuse_last_link_enabled")
    private val streamReuseLastLinkCacheHoursKey = key("stream_reuse_last_link_cache_hours")
    private val decoderPriorityKey = key("decoder_priority")
    private val mapDV7ToHevcKey = key("map_dv7_to_hevc")
    private val tunnelingEnabledKey = key("tunneling_enabled")
    private val streamAutoPlayModeKey = key("stream_auto_play_mode")
    private val streamAutoPlaySourceKey = key("stream_auto_play_source")
    private val streamAutoPlaySelectedAddonsKey = key("stream_auto_play_selected_addons")
    private val streamAutoPlaySelectedPluginsKey = key("stream_auto_play_selected_plugins")
    private val streamAutoPlayRegexKey = key("stream_auto_play_regex")
    private val streamAutoPlayTimeoutSecondsKey = key("stream_auto_play_timeout_seconds")
    private val skipIntroEnabledKey = key("skip_intro_enabled")
    private val animeSkipEnabledKey = key("animeskip_enabled")
    private val animeSkipClientIdKey = key("animeskip_client_id")
    private val introDbApiKeyKey = key("introdb_api_key")
    private val introSubmitEnabledKey = key("intro_submit_enabled")
    private val streamAutoPlayNextEpisodeEnabledKey = key("stream_auto_play_next_episode_enabled")
    private val streamAutoPlayPreferBingeGroupKey = key("stream_auto_play_prefer_binge_group")
    private val streamAutoPlayReuseBingeGroupKey = key("stream_auto_play_reuse_binge_group")
    private val nextEpisodeThresholdModeKey = key("next_episode_threshold_mode")
    private val nextEpisodeThresholdPercentKey = key("next_episode_threshold_percent_v2")
    private val nextEpisodeThresholdMinutesBeforeEndKey = key("next_episode_threshold_minutes_before_end_v2")
    private val useLibassKey = key("use_libass")
    private val libassRenderTypeKey = key("libass_render_type")
    private val episodeCodeFormatKey = key("episode_code_format")
    private val stillWatchingEnabledKey = key("still_watching_enabled")
    private val stillWatchingEpisodeCountKey = key("still_watching_episode_count")
    private val stillWatchingNightModeKey = key("still_watching_night_mode")
    private val swipeGesturesEnabledKey = key("swipe_gestures_enabled")
    private val iosVideoOutputPresetKey = key("ios_video_output_preset")
    private val iosToneMappingModeKey = key("ios_tone_mapping_mode")
    private val iosTargetPrimariesKey = key("ios_target_primaries")
    private val iosTargetTransferKey = key("ios_target_transfer")
    private val iosHardwareDecoderModeKey = key("ios_hardware_decoder_mode")
    private val iosAudioOutputModeKey = key("ios_audio_output_mode")
    private val iosExtendedDynamicRangeEnabledKey = key("ios_extended_dynamic_range_enabled")
    private val iosTargetColorspaceHintEnabledKey = key("ios_target_colorspace_hint_enabled")
    private val iosHdrComputePeakEnabledKey = key("ios_hdr_compute_peak_enabled")
    private val iosDebandEnabledKey = key("ios_deband_enabled")
    private val iosInterpolationEnabledKey = key("ios_interpolation_enabled")
    private val iosBrightnessKey = key("ios_brightness")
    private val iosContrastKey = key("ios_contrast")
    private val iosSaturationKey = key("ios_saturation")
    private val iosGammaKey = key("ios_gamma")
    private val skipSeekIntervalSecondsKey = key("skip_seek_interval_seconds")
    private val volumeBoostDbKey = key("volume_boost_db")

    actual fun loadShowLoadingOverlay(): Boolean? = loadBoolean(showLoadingOverlayKey)
    actual fun saveShowLoadingOverlay(enabled: Boolean) = saveBoolean(showLoadingOverlayKey, enabled)
    actual fun loadResizeMode(): String? = loadString(resizeModeKey)
    actual fun saveResizeMode(mode: String) = saveString(resizeModeKey, mode)
    actual fun loadHoldToSpeedEnabled(): Boolean? = loadBoolean(holdToSpeedEnabledKey)
    actual fun saveHoldToSpeedEnabled(enabled: Boolean) = saveBoolean(holdToSpeedEnabledKey, enabled)
    actual fun loadHoldToSpeedValue(): Float? = loadFloat(holdToSpeedValueKey)
    actual fun saveHoldToSpeedValue(speed: Float) = saveFloat(holdToSpeedValueKey, speed)
    actual fun loadExternalPlayerEnabled(): Boolean? = loadBoolean(externalPlayerEnabledKey)
    actual fun saveExternalPlayerEnabled(enabled: Boolean) = saveBoolean(externalPlayerEnabledKey, enabled)
    actual fun loadExternalPlayerForwardSubtitles(): Boolean? = loadBoolean(externalPlayerForwardSubtitlesKey)
    actual fun saveExternalPlayerForwardSubtitles(enabled: Boolean) = saveBoolean(externalPlayerForwardSubtitlesKey, enabled)
    actual fun loadExternalPlayerId(): String? = loadString(externalPlayerIdKey)
    actual fun saveExternalPlayerId(playerId: String?) = saveNullableString(externalPlayerIdKey, playerId)
    actual fun loadPreferredAudioLanguage(): String? = loadString(preferredAudioLanguageKey)
    actual fun savePreferredAudioLanguage(language: String) = saveString(preferredAudioLanguageKey, language)
    actual fun loadSecondaryPreferredAudioLanguage(): String? = loadString(secondaryPreferredAudioLanguageKey)
    actual fun saveSecondaryPreferredAudioLanguage(language: String?) = saveNullableString(secondaryPreferredAudioLanguageKey, language)
    actual fun loadPreferredSubtitleLanguage(): String? = loadString(preferredSubtitleLanguageKey)
    actual fun savePreferredSubtitleLanguage(language: String) = saveString(preferredSubtitleLanguageKey, language)
    actual fun loadSecondaryPreferredSubtitleLanguage(): String? = loadString(secondaryPreferredSubtitleLanguageKey)
    actual fun saveSecondaryPreferredSubtitleLanguage(language: String?) = saveNullableString(secondaryPreferredSubtitleLanguageKey, language)
    actual fun loadSubtitleTextColor(): String? = loadString(subtitleTextColorKey)
    actual fun saveSubtitleTextColor(colorHex: String) = saveString(subtitleTextColorKey, colorHex)
    actual fun loadSubtitleBackgroundColor(): String? = loadString(subtitleBackgroundColorKey)
    actual fun saveSubtitleBackgroundColor(colorHex: String) = saveString(subtitleBackgroundColorKey, colorHex)
    actual fun loadSubtitleOutlineColor(): String? = loadString(subtitleOutlineColorKey)
    actual fun saveSubtitleOutlineColor(colorHex: String) = saveString(subtitleOutlineColorKey, colorHex)
    actual fun loadSubtitleOutlineEnabled(): Boolean? = loadBoolean(subtitleOutlineEnabledKey)
    actual fun saveSubtitleOutlineEnabled(enabled: Boolean) = saveBoolean(subtitleOutlineEnabledKey, enabled)
    actual fun loadSubtitleOutlineWidth(): Int? = loadInt(subtitleOutlineWidthKey)
    actual fun saveSubtitleOutlineWidth(width: Int) = saveInt(subtitleOutlineWidthKey, width)
    actual fun loadSubtitleBold(): Boolean? = loadBoolean(subtitleBoldKey)
    actual fun saveSubtitleBold(enabled: Boolean) = saveBoolean(subtitleBoldKey, enabled)
    actual fun loadSubtitleFontSizeSp(): Int? = loadInt(subtitleFontSizeSpKey)
    actual fun saveSubtitleFontSizeSp(fontSizeSp: Int) = saveInt(subtitleFontSizeSpKey, fontSizeSp)
    actual fun loadSubtitleBottomOffset(): Int? = loadInt(subtitleBottomOffsetKey)
    actual fun saveSubtitleBottomOffset(bottomOffset: Int) = saveInt(subtitleBottomOffsetKey, bottomOffset)
    actual fun loadSubtitleUseForcedSubtitles(): Boolean? = loadBoolean(subtitleUseForcedSubtitlesKey)
    actual fun saveSubtitleUseForcedSubtitles(enabled: Boolean) = saveBoolean(subtitleUseForcedSubtitlesKey, enabled)
    actual fun loadSubtitleShowOnlyPreferredLanguages(): Boolean? = loadBoolean(subtitleShowOnlyPreferredLanguagesKey)
    actual fun saveSubtitleShowOnlyPreferredLanguages(enabled: Boolean) = saveBoolean(subtitleShowOnlyPreferredLanguagesKey, enabled)
    actual fun loadAddonSubtitleStartupMode(): String? = loadString(addonSubtitleStartupModeKey)
    actual fun saveAddonSubtitleStartupMode(mode: String) = saveString(addonSubtitleStartupModeKey, mode)
    actual fun loadStreamReuseLastLinkEnabled(): Boolean? = loadBoolean(streamReuseLastLinkEnabledKey)
    actual fun saveStreamReuseLastLinkEnabled(enabled: Boolean) = saveBoolean(streamReuseLastLinkEnabledKey, enabled)
    actual fun loadStreamReuseLastLinkCacheHours(): Int? = loadInt(streamReuseLastLinkCacheHoursKey)
    actual fun saveStreamReuseLastLinkCacheHours(hours: Int) = saveInt(streamReuseLastLinkCacheHoursKey, hours)
    actual fun loadDecoderPriority(): Int? = loadInt(decoderPriorityKey)
    actual fun saveDecoderPriority(priority: Int) = saveInt(decoderPriorityKey, priority)
    actual fun loadMapDV7ToHevc(): Boolean? = loadBoolean(mapDV7ToHevcKey)
    actual fun saveMapDV7ToHevc(enabled: Boolean) = saveBoolean(mapDV7ToHevcKey, enabled)
    actual fun loadTunnelingEnabled(): Boolean? = loadBoolean(tunnelingEnabledKey)
    actual fun saveTunnelingEnabled(enabled: Boolean) = saveBoolean(tunnelingEnabledKey, enabled)
    actual fun loadStreamAutoPlayMode(): String? = loadString(streamAutoPlayModeKey)
    actual fun saveStreamAutoPlayMode(mode: String) = saveString(streamAutoPlayModeKey, mode)
    actual fun loadStreamAutoPlaySource(): String? = loadString(streamAutoPlaySourceKey)
    actual fun saveStreamAutoPlaySource(source: String) = saveString(streamAutoPlaySourceKey, source)
    actual fun loadStreamAutoPlaySelectedAddons(): Set<String>? = loadStringSet(streamAutoPlaySelectedAddonsKey)
    actual fun saveStreamAutoPlaySelectedAddons(addons: Set<String>) = saveStringSet(streamAutoPlaySelectedAddonsKey, addons)
    actual fun loadStreamAutoPlaySelectedPlugins(): Set<String>? = loadStringSet(streamAutoPlaySelectedPluginsKey)
    actual fun saveStreamAutoPlaySelectedPlugins(plugins: Set<String>) = saveStringSet(streamAutoPlaySelectedPluginsKey, plugins)
    actual fun loadStreamAutoPlayRegex(): String? = loadString(streamAutoPlayRegexKey)
    actual fun saveStreamAutoPlayRegex(regex: String) = saveString(streamAutoPlayRegexKey, regex)
    actual fun loadStreamAutoPlayTimeoutSeconds(): Int? = loadInt(streamAutoPlayTimeoutSecondsKey)
    actual fun saveStreamAutoPlayTimeoutSeconds(seconds: Int) = saveInt(streamAutoPlayTimeoutSecondsKey, seconds)
    actual fun loadSkipIntroEnabled(): Boolean? = loadBoolean(skipIntroEnabledKey)
    actual fun saveSkipIntroEnabled(enabled: Boolean) = saveBoolean(skipIntroEnabledKey, enabled)
    actual fun loadAnimeSkipEnabled(): Boolean? = loadBoolean(animeSkipEnabledKey)
    actual fun saveAnimeSkipEnabled(enabled: Boolean) = saveBoolean(animeSkipEnabledKey, enabled)
    actual fun loadAnimeSkipClientId(): String? = loadString(animeSkipClientIdKey)
    actual fun saveAnimeSkipClientId(clientId: String) = saveString(animeSkipClientIdKey, clientId)
    actual fun loadIntroDbApiKey(): String? = loadString(introDbApiKeyKey)
    actual fun saveIntroDbApiKey(apiKey: String) = saveString(introDbApiKeyKey, apiKey)
    actual fun loadIntroSubmitEnabled(): Boolean? = loadBoolean(introSubmitEnabledKey)
    actual fun saveIntroSubmitEnabled(enabled: Boolean) = saveBoolean(introSubmitEnabledKey, enabled)
    actual fun loadStreamAutoPlayNextEpisodeEnabled(): Boolean? = loadBoolean(streamAutoPlayNextEpisodeEnabledKey)
    actual fun saveStreamAutoPlayNextEpisodeEnabled(enabled: Boolean) = saveBoolean(streamAutoPlayNextEpisodeEnabledKey, enabled)
    actual fun loadStreamAutoPlayPreferBingeGroup(): Boolean? = loadBoolean(streamAutoPlayPreferBingeGroupKey)
    actual fun saveStreamAutoPlayPreferBingeGroup(enabled: Boolean) = saveBoolean(streamAutoPlayPreferBingeGroupKey, enabled)
    actual fun loadStreamAutoPlayReuseBingeGroup(): Boolean? = loadBoolean(streamAutoPlayReuseBingeGroupKey)
    actual fun saveStreamAutoPlayReuseBingeGroup(enabled: Boolean) = saveBoolean(streamAutoPlayReuseBingeGroupKey, enabled)
    actual fun loadNextEpisodeThresholdMode(): String? = loadString(nextEpisodeThresholdModeKey)
    actual fun saveNextEpisodeThresholdMode(mode: String) = saveString(nextEpisodeThresholdModeKey, mode)
    actual fun loadNextEpisodeThresholdPercent(): Float? = loadFloat(nextEpisodeThresholdPercentKey)
    actual fun saveNextEpisodeThresholdPercent(percent: Float) = saveFloat(nextEpisodeThresholdPercentKey, percent)
    actual fun loadNextEpisodeThresholdMinutesBeforeEnd(): Float? = loadFloat(nextEpisodeThresholdMinutesBeforeEndKey)
    actual fun saveNextEpisodeThresholdMinutesBeforeEnd(minutes: Float) = saveFloat(nextEpisodeThresholdMinutesBeforeEndKey, minutes)
    actual fun loadUseLibass(): Boolean? = loadBoolean(useLibassKey)
    actual fun saveUseLibass(enabled: Boolean) = saveBoolean(useLibassKey, enabled)
    actual fun loadLibassRenderType(): String? = loadString(libassRenderTypeKey)
    actual fun saveLibassRenderType(renderType: String) = saveString(libassRenderTypeKey, renderType)
    actual fun loadEpisodeCodeFormat(): String? = loadString(episodeCodeFormatKey)
    actual fun saveEpisodeCodeFormat(format: String) = saveString(episodeCodeFormatKey, format)
    actual fun loadStillWatchingEnabled(): Boolean? = loadBoolean(stillWatchingEnabledKey)
    actual fun saveStillWatchingEnabled(enabled: Boolean) = saveBoolean(stillWatchingEnabledKey, enabled)
    actual fun loadStillWatchingEpisodeCount(): Int? = loadInt(stillWatchingEpisodeCountKey)
    actual fun saveStillWatchingEpisodeCount(count: Int) = saveInt(stillWatchingEpisodeCountKey, count)
    actual fun loadStillWatchingNightMode(): Boolean? = loadBoolean(stillWatchingNightModeKey)
    actual fun saveStillWatchingNightMode(enabled: Boolean) = saveBoolean(stillWatchingNightModeKey, enabled)
    actual fun loadSwipeGesturesEnabled(): Boolean? = loadBoolean(swipeGesturesEnabledKey)
    actual fun saveSwipeGesturesEnabled(enabled: Boolean) = saveBoolean(swipeGesturesEnabledKey, enabled)
    actual fun loadIosVideoOutputPreset(): String? = loadString(iosVideoOutputPresetKey)
    actual fun saveIosVideoOutputPreset(preset: String) = saveString(iosVideoOutputPresetKey, preset)
    actual fun loadIosToneMappingMode(): String? = loadString(iosToneMappingModeKey)
    actual fun saveIosToneMappingMode(mode: String) = saveString(iosToneMappingModeKey, mode)
    actual fun loadIosTargetPrimaries(): String? = loadString(iosTargetPrimariesKey)
    actual fun saveIosTargetPrimaries(primaries: String) = saveString(iosTargetPrimariesKey, primaries)
    actual fun loadIosTargetTransfer(): String? = loadString(iosTargetTransferKey)
    actual fun saveIosTargetTransfer(transfer: String) = saveString(iosTargetTransferKey, transfer)
    actual fun loadIosHardwareDecoderMode(): String? = loadString(iosHardwareDecoderModeKey)
    actual fun saveIosHardwareDecoderMode(mode: String) = saveString(iosHardwareDecoderModeKey, mode)
    actual fun loadIosAudioOutputMode(): String? = loadString(iosAudioOutputModeKey)
    actual fun saveIosAudioOutputMode(mode: String) = saveString(iosAudioOutputModeKey, mode)
    actual fun loadIosExtendedDynamicRangeEnabled(): Boolean? = loadBoolean(iosExtendedDynamicRangeEnabledKey)
    actual fun saveIosExtendedDynamicRangeEnabled(enabled: Boolean) = saveBoolean(iosExtendedDynamicRangeEnabledKey, enabled)
    actual fun loadIosTargetColorspaceHintEnabled(): Boolean? = loadBoolean(iosTargetColorspaceHintEnabledKey)
    actual fun saveIosTargetColorspaceHintEnabled(enabled: Boolean) = saveBoolean(iosTargetColorspaceHintEnabledKey, enabled)
    actual fun loadIosHdrComputePeakEnabled(): Boolean? = loadBoolean(iosHdrComputePeakEnabledKey)
    actual fun saveIosHdrComputePeakEnabled(enabled: Boolean) = saveBoolean(iosHdrComputePeakEnabledKey, enabled)
    actual fun loadIosDebandEnabled(): Boolean? = loadBoolean(iosDebandEnabledKey)
    actual fun saveIosDebandEnabled(enabled: Boolean) = saveBoolean(iosDebandEnabledKey, enabled)
    actual fun loadIosInterpolationEnabled(): Boolean? = loadBoolean(iosInterpolationEnabledKey)
    actual fun saveIosInterpolationEnabled(enabled: Boolean) = saveBoolean(iosInterpolationEnabledKey, enabled)
    actual fun loadIosBrightness(): Int? = loadInt(iosBrightnessKey)
    actual fun saveIosBrightness(value: Int) = saveInt(iosBrightnessKey, value)
    actual fun loadIosContrast(): Int? = loadInt(iosContrastKey)
    actual fun saveIosContrast(value: Int) = saveInt(iosContrastKey, value)
    actual fun loadIosSaturation(): Int? = loadInt(iosSaturationKey)
    actual fun saveIosSaturation(value: Int) = saveInt(iosSaturationKey, value)
    actual fun loadIosGamma(): Int? = loadInt(iosGammaKey)
    actual fun saveIosGamma(value: Int) = saveInt(iosGammaKey, value)
    actual fun loadSkipSeekIntervalSeconds(): Int? = loadInt(skipSeekIntervalSecondsKey)
    actual fun saveSkipSeekIntervalSeconds(seconds: Int) = saveInt(skipSeekIntervalSecondsKey, seconds)
    actual fun loadVolumeBoostDb(): Int? = loadInt(volumeBoostDbKey)
    actual fun saveVolumeBoostDb(boostDb: Int) = saveInt(volumeBoostDbKey, boostDb)

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadShowLoadingOverlay()?.let { put(showLoadingOverlayKey, encodeSyncBoolean(it)) }
        loadResizeMode()?.let { put(resizeModeKey, encodeSyncString(it)) }
        loadHoldToSpeedEnabled()?.let { put(holdToSpeedEnabledKey, encodeSyncBoolean(it)) }
        loadHoldToSpeedValue()?.let { put(holdToSpeedValueKey, encodeSyncFloat(it)) }
        loadExternalPlayerEnabled()?.let { put(externalPlayerEnabledKey, encodeSyncBoolean(it)) }
        loadExternalPlayerId()?.let { put(externalPlayerIdKey, encodeSyncString(it)) }
        loadPreferredAudioLanguage()?.let { put(preferredAudioLanguageKey, encodeSyncString(it)) }
        loadSecondaryPreferredAudioLanguage()?.let { put(secondaryPreferredAudioLanguageKey, encodeSyncString(it)) }
        loadPreferredSubtitleLanguage()?.let { put(preferredSubtitleLanguageKey, encodeSyncString(it)) }
        loadSecondaryPreferredSubtitleLanguage()?.let { put(secondaryPreferredSubtitleLanguageKey, encodeSyncString(it)) }
        loadSubtitleTextColor()?.let { put(subtitleTextColorKey, encodeSyncString(it)) }
        loadSubtitleOutlineEnabled()?.let { put(subtitleOutlineEnabledKey, encodeSyncBoolean(it)) }
        loadSubtitleFontSizeSp()?.let { put(subtitleFontSizeSpKey, encodeSyncInt(it)) }
        loadSubtitleBottomOffset()?.let { put(subtitleBottomOffsetKey, encodeSyncInt(it)) }
        loadStreamReuseLastLinkEnabled()?.let { put(streamReuseLastLinkEnabledKey, encodeSyncBoolean(it)) }
        loadStreamReuseLastLinkCacheHours()?.let { put(streamReuseLastLinkCacheHoursKey, encodeSyncInt(it)) }
        loadDecoderPriority()?.let { put(decoderPriorityKey, encodeSyncInt(it)) }
        loadMapDV7ToHevc()?.let { put(mapDV7ToHevcKey, encodeSyncBoolean(it)) }
        loadTunnelingEnabled()?.let { put(tunnelingEnabledKey, encodeSyncBoolean(it)) }
        loadStreamAutoPlayMode()?.let { put(streamAutoPlayModeKey, encodeSyncString(it)) }
        loadStreamAutoPlaySource()?.let { put(streamAutoPlaySourceKey, encodeSyncString(it)) }
        loadStreamAutoPlaySelectedAddons()?.let { put(streamAutoPlaySelectedAddonsKey, encodeSyncStringSet(it)) }
        loadStreamAutoPlaySelectedPlugins()?.let { put(streamAutoPlaySelectedPluginsKey, encodeSyncStringSet(it)) }
        loadStreamAutoPlayRegex()?.let { put(streamAutoPlayRegexKey, encodeSyncString(it)) }
        loadStreamAutoPlayTimeoutSeconds()?.let { put(streamAutoPlayTimeoutSecondsKey, encodeSyncInt(it)) }
        loadSkipIntroEnabled()?.let { put(skipIntroEnabledKey, encodeSyncBoolean(it)) }
        loadAnimeSkipEnabled()?.let { put(animeSkipEnabledKey, encodeSyncBoolean(it)) }
        loadAnimeSkipClientId()?.let { put(animeSkipClientIdKey, encodeSyncString(it)) }
        loadIntroDbApiKey()?.let { put(introDbApiKeyKey, encodeSyncString(it)) }
        loadIntroSubmitEnabled()?.let { put(introSubmitEnabledKey, encodeSyncBoolean(it)) }
        loadStreamAutoPlayNextEpisodeEnabled()?.let { put(streamAutoPlayNextEpisodeEnabledKey, encodeSyncBoolean(it)) }
        loadStreamAutoPlayPreferBingeGroup()?.let { put(streamAutoPlayPreferBingeGroupKey, encodeSyncBoolean(it)) }
        loadNextEpisodeThresholdMode()?.let { put(nextEpisodeThresholdModeKey, encodeSyncString(it)) }
        loadNextEpisodeThresholdPercent()?.let { put(nextEpisodeThresholdPercentKey, encodeSyncFloat(it)) }
        loadNextEpisodeThresholdMinutesBeforeEnd()?.let { put(nextEpisodeThresholdMinutesBeforeEndKey, encodeSyncFloat(it)) }
        loadUseLibass()?.let { put(useLibassKey, encodeSyncBoolean(it)) }
        loadLibassRenderType()?.let { put(libassRenderTypeKey, encodeSyncString(it)) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        allKeys.distinct().forEach { WebKeyValueStorage.remove(namespace, ProfileScopedKey.of(it)) }
        payload.decodeSyncBoolean(showLoadingOverlayKey)?.let(::saveShowLoadingOverlay)
        payload.decodeSyncString(resizeModeKey)?.let(::saveResizeMode)
        payload.decodeSyncBoolean(holdToSpeedEnabledKey)?.let(::saveHoldToSpeedEnabled)
        payload.decodeSyncFloat(holdToSpeedValueKey)?.let(::saveHoldToSpeedValue)
        payload.decodeSyncBoolean(externalPlayerEnabledKey)?.let(::saveExternalPlayerEnabled)
        payload.decodeSyncString(externalPlayerIdKey)?.let(::saveExternalPlayerId)
        payload.decodeSyncString(preferredAudioLanguageKey)?.let(::savePreferredAudioLanguage)
        payload.decodeSyncString(secondaryPreferredAudioLanguageKey)?.let(::saveSecondaryPreferredAudioLanguage)
        payload.decodeSyncString(preferredSubtitleLanguageKey)?.let(::savePreferredSubtitleLanguage)
        payload.decodeSyncString(secondaryPreferredSubtitleLanguageKey)?.let(::saveSecondaryPreferredSubtitleLanguage)
        payload.decodeSyncString(subtitleTextColorKey)?.let(::saveSubtitleTextColor)
        payload.decodeSyncBoolean(subtitleOutlineEnabledKey)?.let(::saveSubtitleOutlineEnabled)
        payload.decodeSyncInt(subtitleFontSizeSpKey)?.let(::saveSubtitleFontSizeSp)
        payload.decodeSyncInt(subtitleBottomOffsetKey)?.let(::saveSubtitleBottomOffset)
        payload.decodeSyncBoolean(streamReuseLastLinkEnabledKey)?.let(::saveStreamReuseLastLinkEnabled)
        payload.decodeSyncInt(streamReuseLastLinkCacheHoursKey)?.let(::saveStreamReuseLastLinkCacheHours)
        payload.decodeSyncInt(decoderPriorityKey)?.let(::saveDecoderPriority)
        payload.decodeSyncBoolean(mapDV7ToHevcKey)?.let(::saveMapDV7ToHevc)
        payload.decodeSyncBoolean(tunnelingEnabledKey)?.let(::saveTunnelingEnabled)
        payload.decodeSyncString(streamAutoPlayModeKey)?.let(::saveStreamAutoPlayMode)
        payload.decodeSyncString(streamAutoPlaySourceKey)?.let(::saveStreamAutoPlaySource)
        payload.decodeSyncStringSet(streamAutoPlaySelectedAddonsKey)?.let(::saveStreamAutoPlaySelectedAddons)
        payload.decodeSyncStringSet(streamAutoPlaySelectedPluginsKey)?.let(::saveStreamAutoPlaySelectedPlugins)
        payload.decodeSyncString(streamAutoPlayRegexKey)?.let(::saveStreamAutoPlayRegex)
        payload.decodeSyncInt(streamAutoPlayTimeoutSecondsKey)?.let(::saveStreamAutoPlayTimeoutSeconds)
        payload.decodeSyncBoolean(skipIntroEnabledKey)?.let(::saveSkipIntroEnabled)
        payload.decodeSyncBoolean(animeSkipEnabledKey)?.let(::saveAnimeSkipEnabled)
        payload.decodeSyncString(animeSkipClientIdKey)?.let(::saveAnimeSkipClientId)
        payload.decodeSyncString(introDbApiKeyKey)?.let(::saveIntroDbApiKey)
        payload.decodeSyncBoolean(introSubmitEnabledKey)?.let(::saveIntroSubmitEnabled)
        payload.decodeSyncBoolean(streamAutoPlayNextEpisodeEnabledKey)?.let(::saveStreamAutoPlayNextEpisodeEnabled)
        payload.decodeSyncBoolean(streamAutoPlayPreferBingeGroupKey)?.let(::saveStreamAutoPlayPreferBingeGroup)
        payload.decodeSyncString(nextEpisodeThresholdModeKey)?.let(::saveNextEpisodeThresholdMode)
        payload.decodeSyncFloat(nextEpisodeThresholdPercentKey)?.let(::saveNextEpisodeThresholdPercent)
        payload.decodeSyncFloat(nextEpisodeThresholdMinutesBeforeEndKey)?.let(::saveNextEpisodeThresholdMinutesBeforeEnd)
        payload.decodeSyncBoolean(useLibassKey)?.let(::saveUseLibass)
        payload.decodeSyncString(libassRenderTypeKey)?.let(::saveLibassRenderType)
    }

    private fun scopedKey(key: String): String = ProfileScopedKey.of(key)
    private fun loadBoolean(key: String): Boolean? = WebKeyValueStorage.getBoolean(namespace, scopedKey(key))
    private fun saveBoolean(key: String, value: Boolean) = WebKeyValueStorage.setBoolean(namespace, scopedKey(key), value)
    private fun loadFloat(key: String): Float? = WebKeyValueStorage.getFloat(namespace, scopedKey(key))
    private fun saveFloat(key: String, value: Float) = WebKeyValueStorage.setFloat(namespace, scopedKey(key), value)
    private fun loadInt(key: String): Int? = WebKeyValueStorage.getInt(namespace, scopedKey(key))
    private fun saveInt(key: String, value: Int) = WebKeyValueStorage.setInt(namespace, scopedKey(key), value)
    private fun loadString(key: String): String? = WebKeyValueStorage.getString(namespace, scopedKey(key))
    private fun saveString(key: String, value: String) = WebKeyValueStorage.setString(namespace, scopedKey(key), value)

    private fun saveNullableString(key: String, value: String?) {
        if (value.isNullOrBlank()) {
            WebKeyValueStorage.remove(namespace, scopedKey(key))
        } else {
            saveString(key, value)
        }
    }

    private fun loadStringSet(key: String): Set<String>? {
        val sk = scopedKey(key)
        if (!WebKeyValueStorage.contains(namespace, sk)) return null
        return WebKeyValueStorage.getString(namespace, sk)
            .orEmpty()
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    private fun saveStringSet(key: String, value: Set<String>) =
        WebKeyValueStorage.setString(namespace, scopedKey(key), value.joinToString("\n"))
}
