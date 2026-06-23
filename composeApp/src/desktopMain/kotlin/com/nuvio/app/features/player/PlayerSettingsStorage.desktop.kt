package com.nuvio.app.features.player

import com.nuvio.app.core.storage.DesktopStorage
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

internal actual object PlayerSettingsStorage {
    private val store = DesktopStorage.store("nuvio_player_settings")
    private val json = Json { ignoreUnknownKeys = true }

    actual fun loadShowLoadingOverlay(): Boolean? = store.getBoolean("show_loading_overlay")
    actual fun saveShowLoadingOverlay(enabled: Boolean) { store.putBoolean("show_loading_overlay", enabled) }
    actual fun loadResizeMode(): String? = store.getString("resize_mode")
    actual fun saveResizeMode(mode: String) { store.putString("resize_mode", mode) }
    actual fun loadHoldToSpeedEnabled(): Boolean? = store.getBoolean("hold_to_speed_enabled")
    actual fun saveHoldToSpeedEnabled(enabled: Boolean) { store.putBoolean("hold_to_speed_enabled", enabled) }
    actual fun loadHoldToSpeedValue(): Float? = store.getFloat("hold_to_speed_value")
    actual fun saveHoldToSpeedValue(speed: Float) { store.putFloat("hold_to_speed_value", speed) }
    actual fun loadTouchGesturesEnabled(): Boolean? = store.getBoolean("touch_gestures_enabled")
    actual fun saveTouchGesturesEnabled(enabled: Boolean) { store.putBoolean("touch_gestures_enabled", enabled) }
    actual fun loadExternalPlayerEnabled(): Boolean? = store.getBoolean("external_player_enabled")
    actual fun saveExternalPlayerEnabled(enabled: Boolean) { store.putBoolean("external_player_enabled", enabled) }
    actual fun loadExternalPlayerForwardSubtitles(): Boolean? = store.getBoolean("external_player_forward_subtitles")
    actual fun saveExternalPlayerForwardSubtitles(enabled: Boolean) { store.putBoolean("external_player_forward_subtitles", enabled) }
    actual fun loadExternalPlayerId(): String? = store.getString("external_player_id")
    actual fun saveExternalPlayerId(playerId: String?) { store.putString("external_player_id", playerId) }
    actual fun loadPreferredAudioLanguage(): String? = store.getString("preferred_audio_language")
    actual fun savePreferredAudioLanguage(language: String) { store.putString("preferred_audio_language", language) }
    actual fun loadSecondaryPreferredAudioLanguage(): String? = store.getString("secondary_preferred_audio_language")
    actual fun saveSecondaryPreferredAudioLanguage(language: String?) { store.putString("secondary_preferred_audio_language", language) }
    actual fun loadPreferredSubtitleLanguage(): String? = store.getString("preferred_subtitle_language")
    actual fun savePreferredSubtitleLanguage(language: String) { store.putString("preferred_subtitle_language", language) }
    actual fun loadSecondaryPreferredSubtitleLanguage(): String? = store.getString("secondary_preferred_subtitle_language")
    actual fun saveSecondaryPreferredSubtitleLanguage(language: String?) { store.putString("secondary_preferred_subtitle_language", language) }
    actual fun loadSubtitleTextColor(): String? = store.getString("subtitle_text_color")
    actual fun saveSubtitleTextColor(colorHex: String) { store.putString("subtitle_text_color", colorHex) }
    actual fun loadSubtitleBackgroundColor(): String? = store.getString("subtitle_background_color")
    actual fun saveSubtitleBackgroundColor(colorHex: String) { store.putString("subtitle_background_color", colorHex) }
    actual fun loadSubtitleOutlineColor(): String? = store.getString("subtitle_outline_color")
    actual fun saveSubtitleOutlineColor(colorHex: String) { store.putString("subtitle_outline_color", colorHex) }
    actual fun loadSubtitleOutlineEnabled(): Boolean? = store.getBoolean("subtitle_outline_enabled")
    actual fun saveSubtitleOutlineEnabled(enabled: Boolean) { store.putBoolean("subtitle_outline_enabled", enabled) }
    actual fun loadSubtitleOutlineWidth(): Int? = store.getInt("subtitle_outline_width")
    actual fun saveSubtitleOutlineWidth(width: Int) { store.putInt("subtitle_outline_width", width) }
    actual fun loadSubtitleBold(): Boolean? = store.getBoolean("subtitle_bold")
    actual fun saveSubtitleBold(enabled: Boolean) { store.putBoolean("subtitle_bold", enabled) }
    actual fun loadSubtitleFontSizeSp(): Int? = store.getInt("subtitle_font_size_sp")
    actual fun saveSubtitleFontSizeSp(fontSizeSp: Int) { store.putInt("subtitle_font_size_sp", fontSizeSp) }
    actual fun loadSubtitleBottomOffset(): Int? = store.getInt("subtitle_bottom_offset")
    actual fun saveSubtitleBottomOffset(bottomOffset: Int) { store.putInt("subtitle_bottom_offset", bottomOffset) }
    actual fun loadSubtitleUseForcedSubtitles(): Boolean? = store.getBoolean("subtitle_use_forced_subtitles")
    actual fun saveSubtitleUseForcedSubtitles(enabled: Boolean) { store.putBoolean("subtitle_use_forced_subtitles", enabled) }
    actual fun loadSubtitleShowOnlyPreferredLanguages(): Boolean? = store.getBoolean("subtitle_show_only_preferred_languages")
    actual fun saveSubtitleShowOnlyPreferredLanguages(enabled: Boolean) { store.putBoolean("subtitle_show_only_preferred_languages", enabled) }
    actual fun loadAddonSubtitleStartupMode(): String? = store.getString("addon_subtitle_startup_mode")
    actual fun saveAddonSubtitleStartupMode(mode: String) { store.putString("addon_subtitle_startup_mode", mode) }
    actual fun loadStreamReuseLastLinkEnabled(): Boolean? = store.getBoolean("stream_reuse_last_link_enabled")
    actual fun saveStreamReuseLastLinkEnabled(enabled: Boolean) { store.putBoolean("stream_reuse_last_link_enabled", enabled) }
    actual fun loadStreamReuseLastLinkCacheHours(): Int? = store.getInt("stream_reuse_last_link_cache_hours")
    actual fun saveStreamReuseLastLinkCacheHours(hours: Int) { store.putInt("stream_reuse_last_link_cache_hours", hours) }
    actual fun loadAndroidPlaybackEngine(): String? = null
    actual fun saveAndroidPlaybackEngine(engine: String) {}
    actual fun loadAndroidLibmpvVideoOutput(): String? = null
    actual fun saveAndroidLibmpvVideoOutput(output: String) {}
    actual fun loadAndroidLibmpvHardwareDecodingEnabled(): Boolean? = null
    actual fun saveAndroidLibmpvHardwareDecodingEnabled(enabled: Boolean) {}
    actual fun loadAndroidLibmpvYuv420pEnabled(): Boolean? = null
    actual fun saveAndroidLibmpvYuv420pEnabled(enabled: Boolean) {}
    actual fun loadDecoderPriority(): Int? = store.getInt("decoder_priority")
    actual fun saveDecoderPriority(priority: Int) { store.putInt("decoder_priority", priority) }
    actual fun loadMapDV7ToHevc(): Boolean? = store.getBoolean("map_dv7_to_hevc")
    actual fun saveMapDV7ToHevc(enabled: Boolean) { store.putBoolean("map_dv7_to_hevc", enabled) }
    actual fun loadTunnelingEnabled(): Boolean? = store.getBoolean("tunneling_enabled")
    actual fun saveTunnelingEnabled(enabled: Boolean) { store.putBoolean("tunneling_enabled", enabled) }
    actual fun loadStreamAutoPlayMode(): String? = store.getString("stream_auto_play_mode")
    actual fun saveStreamAutoPlayMode(mode: String) { store.putString("stream_auto_play_mode", mode) }
    actual fun loadStreamAutoPlaySource(): String? = store.getString("stream_auto_play_source")
    actual fun saveStreamAutoPlaySource(source: String) { store.putString("stream_auto_play_source", source) }
    actual fun loadStreamAutoPlaySelectedAddons(): Set<String>? = store.getStringSet("stream_auto_play_selected_addons")
    actual fun saveStreamAutoPlaySelectedAddons(addons: Set<String>) { store.putStringSet("stream_auto_play_selected_addons", addons) }
    actual fun loadStreamAutoPlaySelectedPlugins(): Set<String>? = store.getStringSet("stream_auto_play_selected_plugins")
    actual fun saveStreamAutoPlaySelectedPlugins(plugins: Set<String>) { store.putStringSet("stream_auto_play_selected_plugins", plugins) }
    actual fun loadStreamAutoPlayRegex(): String? = store.getString("stream_auto_play_regex")
    actual fun saveStreamAutoPlayRegex(regex: String) { store.putString("stream_auto_play_regex", regex) }
    actual fun loadStreamAutoPlayTimeoutSeconds(): Int? = store.getInt("stream_auto_play_timeout_seconds")
    actual fun saveStreamAutoPlayTimeoutSeconds(seconds: Int) { store.putInt("stream_auto_play_timeout_seconds", seconds) }
    actual fun loadSkipIntroEnabled(): Boolean? = store.getBoolean("skip_intro_enabled")
    actual fun saveSkipIntroEnabled(enabled: Boolean) { store.putBoolean("skip_intro_enabled", enabled) }
    actual fun loadAnimeSkipEnabled(): Boolean? = store.getBoolean("anime_skip_enabled")
    actual fun saveAnimeSkipEnabled(enabled: Boolean) { store.putBoolean("anime_skip_enabled", enabled) }
    actual fun loadAnimeSkipClientId(): String? = store.getString("anime_skip_client_id")
    actual fun saveAnimeSkipClientId(clientId: String) { store.putString("anime_skip_client_id", clientId) }
    actual fun loadIntroDbApiKey(): String? = store.getString("intro_db_api_key")
    actual fun saveIntroDbApiKey(apiKey: String) { store.putString("intro_db_api_key", apiKey) }
    actual fun loadIntroSubmitEnabled(): Boolean? = store.getBoolean("intro_submit_enabled")
    actual fun saveIntroSubmitEnabled(enabled: Boolean) { store.putBoolean("intro_submit_enabled", enabled) }
    actual fun loadStreamAutoPlayNextEpisodeEnabled(): Boolean? = store.getBoolean("stream_auto_play_next_episode_enabled")
    actual fun saveStreamAutoPlayNextEpisodeEnabled(enabled: Boolean) { store.putBoolean("stream_auto_play_next_episode_enabled", enabled) }
    actual fun loadStreamAutoPlayPreferBingeGroup(): Boolean? = store.getBoolean("stream_auto_play_prefer_binge_group")
    actual fun saveStreamAutoPlayPreferBingeGroup(enabled: Boolean) { store.putBoolean("stream_auto_play_prefer_binge_group", enabled) }
    actual fun loadStreamAutoPlayReuseBingeGroup(): Boolean? = store.getBoolean("stream_auto_play_reuse_binge_group")
    actual fun saveStreamAutoPlayReuseBingeGroup(enabled: Boolean) { store.putBoolean("stream_auto_play_reuse_binge_group", enabled) }
    actual fun loadNextEpisodeThresholdMode(): String? = store.getString("next_episode_threshold_mode")
    actual fun saveNextEpisodeThresholdMode(mode: String) { store.putString("next_episode_threshold_mode", mode) }
    actual fun loadNextEpisodeThresholdPercent(): Float? = store.getFloat("next_episode_threshold_percent")
    actual fun saveNextEpisodeThresholdPercent(percent: Float) { store.putFloat("next_episode_threshold_percent", percent) }
    actual fun loadNextEpisodeThresholdMinutesBeforeEnd(): Float? = store.getFloat("next_episode_threshold_minutes_before_end")
    actual fun saveNextEpisodeThresholdMinutesBeforeEnd(minutes: Float) { store.putFloat("next_episode_threshold_minutes_before_end", minutes) }
    actual fun loadUseLibass(): Boolean? = store.getBoolean("use_libass")
    actual fun saveUseLibass(enabled: Boolean) { store.putBoolean("use_libass", enabled) }
    actual fun loadLibassRenderType(): String? = store.getString("libass_render_type")
    actual fun saveLibassRenderType(renderType: String) { store.putString("libass_render_type", renderType) }
    actual fun loadEpisodeCodeFormat(): String? = store.getString("episode_code_format")
    actual fun saveEpisodeCodeFormat(format: String) { store.putString("episode_code_format", format) }
    actual fun loadStillWatchingEnabled(): Boolean? = store.getBoolean("still_watching_enabled")
    actual fun saveStillWatchingEnabled(enabled: Boolean) { store.putBoolean("still_watching_enabled", enabled) }
    actual fun loadStillWatchingEpisodeCount(): Int? = store.getInt("still_watching_episode_count")
    actual fun saveStillWatchingEpisodeCount(count: Int) { store.putInt("still_watching_episode_count", count) }
    actual fun loadStillWatchingNightMode(): Boolean? = store.getBoolean("still_watching_night_mode")
    actual fun saveStillWatchingNightMode(enabled: Boolean) { store.putBoolean("still_watching_night_mode", enabled) }
    actual fun loadIosVideoOutputPreset(): String? = store.getString("ios_video_output_preset")
    actual fun saveIosVideoOutputPreset(preset: String) { store.putString("ios_video_output_preset", preset) }
    actual fun loadIosToneMappingMode(): String? = store.getString("ios_tone_mapping_mode")
    actual fun saveIosToneMappingMode(mode: String) { store.putString("ios_tone_mapping_mode", mode) }
    actual fun loadIosTargetPrimaries(): String? = store.getString("ios_target_primaries")
    actual fun saveIosTargetPrimaries(primaries: String) { store.putString("ios_target_primaries", primaries) }
    actual fun loadIosTargetTransfer(): String? = store.getString("ios_target_transfer")
    actual fun saveIosTargetTransfer(transfer: String) { store.putString("ios_target_transfer", transfer) }
    actual fun loadIosHardwareDecoderMode(): String? = store.getString("ios_hardware_decoder_mode")
    actual fun saveIosHardwareDecoderMode(mode: String) { store.putString("ios_hardware_decoder_mode", mode) }
    actual fun loadIosAudioOutputMode(): String? = store.getString("ios_audio_output_mode")
    actual fun saveIosAudioOutputMode(mode: String) { store.putString("ios_audio_output_mode", mode) }
    actual fun loadIosExtendedDynamicRangeEnabled(): Boolean? = store.getBoolean("ios_extended_dynamic_range_enabled")
    actual fun saveIosExtendedDynamicRangeEnabled(enabled: Boolean) { store.putBoolean("ios_extended_dynamic_range_enabled", enabled) }
    actual fun loadIosTargetColorspaceHintEnabled(): Boolean? = store.getBoolean("ios_target_colorspace_hint_enabled")
    actual fun saveIosTargetColorspaceHintEnabled(enabled: Boolean) { store.putBoolean("ios_target_colorspace_hint_enabled", enabled) }
    actual fun loadIosHdrComputePeakEnabled(): Boolean? = store.getBoolean("ios_hdr_compute_peak_enabled")
    actual fun saveIosHdrComputePeakEnabled(enabled: Boolean) { store.putBoolean("ios_hdr_compute_peak_enabled", enabled) }
    actual fun loadIosDebandEnabled(): Boolean? = store.getBoolean("ios_deband_enabled")
    actual fun saveIosDebandEnabled(enabled: Boolean) { store.putBoolean("ios_deband_enabled", enabled) }
    actual fun loadIosInterpolationEnabled(): Boolean? = store.getBoolean("ios_interpolation_enabled")
    actual fun saveIosInterpolationEnabled(enabled: Boolean) { store.putBoolean("ios_interpolation_enabled", enabled) }
    actual fun loadIosBrightness(): Int? = store.getInt("ios_brightness")
    actual fun saveIosBrightness(value: Int) { store.putInt("ios_brightness", value) }
    actual fun loadIosContrast(): Int? = store.getInt("ios_contrast")
    actual fun saveIosContrast(value: Int) { store.putInt("ios_contrast", value) }
    actual fun loadIosSaturation(): Int? = store.getInt("ios_saturation")
    actual fun saveIosSaturation(value: Int) { store.putInt("ios_saturation", value) }
    actual fun loadIosGamma(): Int? = store.getInt("ios_gamma")
    actual fun saveIosGamma(value: Int) { store.putInt("ios_gamma", value) }
    actual fun loadSkipSeekIntervalSeconds(): Int? = store.getInt("skip_seek_interval_seconds")
    actual fun saveSkipSeekIntervalSeconds(seconds: Int) { store.putInt("skip_seek_interval_seconds", seconds) }
    actual fun loadVolumeBoostDb(): Int? = store.getInt("volume_boost_db")
    actual fun saveVolumeBoostDb(boostDb: Int) { store.putInt("volume_boost_db", boostDb) }

    actual fun exportToSyncPayload(): JsonObject {
        val map = mutableMapOf<String, String>()
        fun putOpt(key: String, value: Any?) {
            if (value != null) map[key] = value.toString()
        }
        putOpt("show_loading_overlay", loadShowLoadingOverlay())
        putOpt("resize_mode", loadResizeMode())
        putOpt("hold_to_speed_enabled", loadHoldToSpeedEnabled())
        putOpt("hold_to_speed_value", loadHoldToSpeedValue())
        putOpt("touch_gestures_enabled", loadTouchGesturesEnabled())
        putOpt("external_player_enabled", loadExternalPlayerEnabled())
        putOpt("external_player_forward_subtitles", loadExternalPlayerForwardSubtitles())
        putOpt("external_player_id", loadExternalPlayerId())
        putOpt("preferred_audio_language", loadPreferredAudioLanguage())
        putOpt("secondary_preferred_audio_language", loadSecondaryPreferredAudioLanguage())
        putOpt("preferred_subtitle_language", loadPreferredSubtitleLanguage())
        putOpt("secondary_preferred_subtitle_language", loadSecondaryPreferredSubtitleLanguage())
        putOpt("subtitle_text_color", loadSubtitleTextColor())
        putOpt("subtitle_background_color", loadSubtitleBackgroundColor())
        putOpt("subtitle_outline_color", loadSubtitleOutlineColor())
        putOpt("subtitle_outline_enabled", loadSubtitleOutlineEnabled())
        putOpt("subtitle_outline_width", loadSubtitleOutlineWidth())
        putOpt("subtitle_bold", loadSubtitleBold())
        putOpt("subtitle_font_size_sp", loadSubtitleFontSizeSp())
        putOpt("subtitle_bottom_offset", loadSubtitleBottomOffset())
        putOpt("subtitle_use_forced_subtitles", loadSubtitleUseForcedSubtitles())
        putOpt("subtitle_show_only_preferred_languages", loadSubtitleShowOnlyPreferredLanguages())
        putOpt("addon_subtitle_startup_mode", loadAddonSubtitleStartupMode())
        putOpt("stream_reuse_last_link_enabled", loadStreamReuseLastLinkEnabled())
        putOpt("stream_reuse_last_link_cache_hours", loadStreamReuseLastLinkCacheHours())
        putOpt("decoder_priority", loadDecoderPriority())
        putOpt("map_dv7_to_hevc", loadMapDV7ToHevc())
        putOpt("tunneling_enabled", loadTunnelingEnabled())
        putOpt("stream_auto_play_mode", loadStreamAutoPlayMode())
        putOpt("stream_auto_play_source", loadStreamAutoPlaySource())
        putOpt("stream_auto_play_regex", loadStreamAutoPlayRegex())
        putOpt("stream_auto_play_timeout_seconds", loadStreamAutoPlayTimeoutSeconds())
        putOpt("skip_intro_enabled", loadSkipIntroEnabled())
        putOpt("anime_skip_enabled", loadAnimeSkipEnabled())
        putOpt("anime_skip_client_id", loadAnimeSkipClientId())
        putOpt("intro_db_api_key", loadIntroDbApiKey())
        putOpt("intro_submit_enabled", loadIntroSubmitEnabled())
        putOpt("stream_auto_play_next_episode_enabled", loadStreamAutoPlayNextEpisodeEnabled())
        putOpt("stream_auto_play_prefer_binge_group", loadStreamAutoPlayPreferBingeGroup())
        putOpt("stream_auto_play_reuse_binge_group", loadStreamAutoPlayReuseBingeGroup())
        putOpt("next_episode_threshold_mode", loadNextEpisodeThresholdMode())
        putOpt("next_episode_threshold_percent", loadNextEpisodeThresholdPercent())
        putOpt("next_episode_threshold_minutes_before_end", loadNextEpisodeThresholdMinutesBeforeEnd())
        putOpt("use_libass", loadUseLibass())
        putOpt("libass_render_type", loadLibassRenderType())
        putOpt("episode_code_format", loadEpisodeCodeFormat())
        putOpt("still_watching_enabled", loadStillWatchingEnabled())
        putOpt("still_watching_episode_count", loadStillWatchingEpisodeCount())
        putOpt("still_watching_night_mode", loadStillWatchingNightMode())
        putOpt("volume_boost_db", loadVolumeBoostDb())
        putOpt("skip_seek_interval_seconds", loadSkipSeekIntervalSeconds())
        return json.decodeFromString(json.encodeToString(map))
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        fun bool(key: String) = payload[key]?.toString()?.toBooleanStrictOrNull()
        fun int(key: String) = payload[key]?.toString()?.toIntOrNull()
        fun float(key: String) = payload[key]?.toString()?.toFloatOrNull()
        fun str(key: String) = payload[key]?.toString()

        bool("show_loading_overlay")?.let { saveShowLoadingOverlay(it) }
        str("resize_mode")?.let { saveResizeMode(it) }
        bool("hold_to_speed_enabled")?.let { saveHoldToSpeedEnabled(it) }
        float("hold_to_speed_value")?.let { saveHoldToSpeedValue(it) }
        bool("touch_gestures_enabled")?.let { saveTouchGesturesEnabled(it) }
        bool("external_player_enabled")?.let { saveExternalPlayerEnabled(it) }
        bool("external_player_forward_subtitles")?.let { saveExternalPlayerForwardSubtitles(it) }
        str("external_player_id")?.let { saveExternalPlayerId(it) }
        str("preferred_audio_language")?.let { savePreferredAudioLanguage(it) }
        str("secondary_preferred_audio_language")?.let { saveSecondaryPreferredAudioLanguage(it) }
        str("preferred_subtitle_language")?.let { savePreferredSubtitleLanguage(it) }
        str("secondary_preferred_subtitle_language")?.let { saveSecondaryPreferredSubtitleLanguage(it) }
        str("subtitle_text_color")?.let { saveSubtitleTextColor(it) }
        str("subtitle_background_color")?.let { saveSubtitleBackgroundColor(it) }
        str("subtitle_outline_color")?.let { saveSubtitleOutlineColor(it) }
        bool("subtitle_outline_enabled")?.let { saveSubtitleOutlineEnabled(it) }
        int("subtitle_outline_width")?.let { saveSubtitleOutlineWidth(it) }
        bool("subtitle_bold")?.let { saveSubtitleBold(it) }
        int("subtitle_font_size_sp")?.let { saveSubtitleFontSizeSp(it) }
        int("subtitle_bottom_offset")?.let { saveSubtitleBottomOffset(it) }
        bool("subtitle_use_forced_subtitles")?.let { saveSubtitleUseForcedSubtitles(it) }
        bool("subtitle_show_only_preferred_languages")?.let { saveSubtitleShowOnlyPreferredLanguages(it) }
        str("addon_subtitle_startup_mode")?.let { saveAddonSubtitleStartupMode(it) }
        bool("stream_reuse_last_link_enabled")?.let { saveStreamReuseLastLinkEnabled(it) }
        int("stream_reuse_last_link_cache_hours")?.let { saveStreamReuseLastLinkCacheHours(it) }
        int("decoder_priority")?.let { saveDecoderPriority(it) }
        bool("map_dv7_to_hevc")?.let { saveMapDV7ToHevc(it) }
        bool("tunneling_enabled")?.let { saveTunnelingEnabled(it) }
        str("stream_auto_play_mode")?.let { saveStreamAutoPlayMode(it) }
        str("stream_auto_play_source")?.let { saveStreamAutoPlaySource(it) }
        str("stream_auto_play_regex")?.let { saveStreamAutoPlayRegex(it) }
        int("stream_auto_play_timeout_seconds")?.let { saveStreamAutoPlayTimeoutSeconds(it) }
        bool("skip_intro_enabled")?.let { saveSkipIntroEnabled(it) }
        bool("anime_skip_enabled")?.let { saveAnimeSkipEnabled(it) }
        str("anime_skip_client_id")?.let { saveAnimeSkipClientId(it) }
        str("intro_db_api_key")?.let { saveIntroDbApiKey(it) }
        bool("intro_submit_enabled")?.let { saveIntroSubmitEnabled(it) }
        bool("stream_auto_play_next_episode_enabled")?.let { saveStreamAutoPlayNextEpisodeEnabled(it) }
        bool("stream_auto_play_prefer_binge_group")?.let { saveStreamAutoPlayPreferBingeGroup(it) }
        bool("stream_auto_play_reuse_binge_group")?.let { saveStreamAutoPlayReuseBingeGroup(it) }
        str("next_episode_threshold_mode")?.let { saveNextEpisodeThresholdMode(it) }
        float("next_episode_threshold_percent")?.let { saveNextEpisodeThresholdPercent(it) }
        float("next_episode_threshold_minutes_before_end")?.let { saveNextEpisodeThresholdMinutesBeforeEnd(it) }
        bool("use_libass")?.let { saveUseLibass(it) }
        str("libass_render_type")?.let { saveLibassRenderType(it) }
        str("episode_code_format")?.let { saveEpisodeCodeFormat(it) }
        bool("still_watching_enabled")?.let { saveStillWatchingEnabled(it) }
        int("still_watching_episode_count")?.let { saveStillWatchingEpisodeCount(it) }
        bool("still_watching_night_mode")?.let { saveStillWatchingNightMode(it) }
        int("volume_boost_db")?.let { saveVolumeBoostDb(it) }
        int("skip_seek_interval_seconds")?.let { saveSkipSeekIntervalSeconds(it) }
    }
}
