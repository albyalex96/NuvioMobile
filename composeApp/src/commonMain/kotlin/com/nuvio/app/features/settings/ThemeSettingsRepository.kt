package com.nuvio.app.features.settings

import androidx.compose.ui.graphics.Color
import com.nuvio.app.core.format.DateFormatOption
import com.nuvio.app.core.ui.AppTheme
import com.nuvio.app.core.ui.NativeTabBridge
import com.nuvio.app.core.ui.ThemeAccentColor
import com.nuvio.app.core.ui.ThemeAnimationStyle
import com.nuvio.app.core.ui.ThemeColors
import com.nuvio.app.core.ui.toThemeHex
import com.nuvio.app.core.ui.toThemeColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeSettingsRepository {
    private val _selectedTheme = MutableStateFlow(AppTheme.WHITE)
    val selectedTheme: StateFlow<AppTheme> = _selectedTheme.asStateFlow()

    private val _customAccentHex = MutableStateFlow("#1E88E5")
    val customAccentHex: StateFlow<String> = _customAccentHex.asStateFlow()

    private val _customThemeFirstColor = MutableStateFlow(ThemeAccentColor.PINK.color)
    val customThemeFirstColor: StateFlow<Color> = _customThemeFirstColor.asStateFlow()

    private val _customThemeSecondColor = MutableStateFlow(ThemeAccentColor.CYAN.color)
    val customThemeSecondColor: StateFlow<Color> = _customThemeSecondColor.asStateFlow()

    private val _themeAnimationStyle = MutableStateFlow(ThemeAnimationStyle.FLOW)
    val themeAnimationStyle: StateFlow<ThemeAnimationStyle> = _themeAnimationStyle.asStateFlow()

    private val _selectedAppIconId = MutableStateFlow("default")
    val selectedAppIconId: StateFlow<String> = _selectedAppIconId.asStateFlow()

    private val _amoledEnabled = MutableStateFlow(false)
    val amoledEnabled: StateFlow<Boolean> = _amoledEnabled.asStateFlow()

    private val _amoledSurfacesEnabled = MutableStateFlow(false)
    val amoledSurfacesEnabled: StateFlow<Boolean> = _amoledSurfacesEnabled.asStateFlow()

    private val _liquidGlassNativeTabBarEnabled = MutableStateFlow(false)
    val liquidGlassNativeTabBarEnabled: StateFlow<Boolean> = _liquidGlassNativeTabBarEnabled.asStateFlow()

    private val _glassNavBarEnabled = MutableStateFlow(false)
    val glassNavBarEnabled: StateFlow<Boolean> = _glassNavBarEnabled.asStateFlow()

    private val _selectedAppLanguage = MutableStateFlow(AppLanguage.DEVICE)
    val selectedAppLanguage: StateFlow<AppLanguage> = _selectedAppLanguage.asStateFlow()

    private val _dateFormatOption = MutableStateFlow(DateFormatOption.YEAR_MONTH_DAY_TEXT)
    val dateFormatOption: StateFlow<DateFormatOption> = _dateFormatOption.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun clearLocalState() {
        hasLoaded = false
        _selectedTheme.value = AppTheme.WHITE
        _customAccentHex.value = "#1E88E5"
        _customThemeFirstColor.value = ThemeAccentColor.PINK.color
        _customThemeSecondColor.value = ThemeAccentColor.CYAN.color
        _themeAnimationStyle.value = ThemeAnimationStyle.FLOW
        _selectedAppIconId.value = "default"
        _amoledEnabled.value = false
        _amoledSurfacesEnabled.value = false
        _liquidGlassNativeTabBarEnabled.value = false
        _glassNavBarEnabled.value = false
        NativeTabBridge.publishAccentColor(nativeTabAccentHex(AppTheme.WHITE, "#1E88E5"))
        NativeTabBridge.publishLiquidGlassEnabled(false)
        _selectedAppLanguage.value = AppLanguage.DEVICE
        _dateFormatOption.value = DateFormatOption.YEAR_MONTH_DAY_TEXT
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val stored = ThemeSettingsStorage.loadSelectedTheme()
        val theme = if (stored != null) {
            try {
                val migrated = migrateLegacyThemeName(stored)
                AppTheme.valueOf(migrated)
            } catch (_: IllegalArgumentException) {
                AppTheme.WHITE
            }
        } else {
            AppTheme.WHITE
        }
        _selectedTheme.value = theme
        _customAccentHex.value = ThemeSettingsStorage.loadCustomAccentHex() ?: "#1E88E5"
        NativeTabBridge.publishAccentColor(nativeTabAccentHex(theme, _customAccentHex.value))
        _customThemeFirstColor.value = ThemeSettingsStorage.loadCustomThemeFirstColor()
            .toThemeColor(ThemeAccentColor.PINK.color)
        _customThemeSecondColor.value = ThemeSettingsStorage.loadCustomThemeSecondColor()
            .toThemeColor(ThemeAccentColor.CYAN.color)
        _themeAnimationStyle.value = ThemeSettingsStorage.loadThemeAnimationStyle()
            ?.let { runCatching { ThemeAnimationStyle.valueOf(it) }.getOrNull() }
            ?: ThemeAnimationStyle.FLOW
        val loadedIconId = ThemeSettingsStorage.loadSelectedAppIconId() ?: "default"
        _selectedAppIconId.value = loadedIconId
        NuvioAppIconSwitcher.reapply(loadedIconId)
        _amoledEnabled.value = ThemeSettingsStorage.loadAmoledEnabled() ?: false
        _amoledSurfacesEnabled.value = ThemeSettingsStorage.loadAmoledSurfacesEnabled() ?: false
        val liquidGlassEnabled = ThemeSettingsStorage.loadLiquidGlassNativeTabBarEnabled() ?: false
        _liquidGlassNativeTabBarEnabled.value = liquidGlassEnabled
        NativeTabBridge.publishLiquidGlassEnabled(liquidGlassEnabled)
        _glassNavBarEnabled.value = ThemeSettingsStorage.loadGlassNavBarEnabled() ?: false
        val appLanguage = AppLanguage.fromCode(ThemeSettingsStorage.loadSelectedAppLanguage())
        ThemeSettingsStorage.applySelectedAppLanguage(appLanguage.code)
        _selectedAppLanguage.value = appLanguage
        _dateFormatOption.value = ThemeSettingsStorage.loadDateFormatOption()
            ?.let { runCatching { DateFormatOption.valueOf(it) }.getOrNull() }
            ?: DateFormatOption.YEAR_MONTH_DAY_TEXT
    }

    private fun migrateLegacyThemeName(name: String): String = when (name) {
        "AURORA" -> "MESSENGER"
        else -> name
    }

    fun setTheme(theme: AppTheme) {
        ensureLoaded()
        if (_selectedTheme.value == theme) return
        _selectedTheme.value = theme
        ThemeSettingsStorage.saveSelectedTheme(theme.name)
        NativeTabBridge.publishAccentColor(nativeTabAccentHex(theme, _customAccentHex.value))
    }

    fun setCustomAccentColor(hex: String) {
        ensureLoaded()
        if (_customAccentHex.value == hex) return
        _customAccentHex.value = hex
        ThemeSettingsStorage.saveCustomAccentHex(hex)
        if (_selectedTheme.value == AppTheme.CUSTOM) {
            NativeTabBridge.publishAccentColor(hex)
        }
    }

    fun setCustomThemeFirstColor(color: Color) {
        ensureLoaded()
        if (_customThemeFirstColor.value == color) return
        _customThemeFirstColor.value = color
        ThemeSettingsStorage.saveCustomThemeFirstColor(color.toThemeHex())
    }

    fun setCustomThemeSecondColor(color: Color) {
        ensureLoaded()
        if (_customThemeSecondColor.value == color) return
        _customThemeSecondColor.value = color
        ThemeSettingsStorage.saveCustomThemeSecondColor(color.toThemeHex())
    }

    fun setThemeAnimationStyle(style: ThemeAnimationStyle) {
        ensureLoaded()
        if (_themeAnimationStyle.value == style) return
        _themeAnimationStyle.value = style
        ThemeSettingsStorage.saveThemeAnimationStyle(style.name)
    }

    fun setAppIcon(iconId: String) {
        ensureLoaded()
        if (_selectedAppIconId.value == iconId) return
        _selectedAppIconId.value = iconId
        ThemeSettingsStorage.saveSelectedAppIconId(iconId)
        NuvioAppIconSwitcher.apply(iconId)
        NuvioAppIconSwitcher.closeAfterApply()
    }

    fun setAmoled(enabled: Boolean) {
        ensureLoaded()
        if (_amoledEnabled.value == enabled) return
        _amoledEnabled.value = enabled
        ThemeSettingsStorage.saveAmoledEnabled(enabled)
        if (!enabled) {
            _amoledSurfacesEnabled.value = false
            ThemeSettingsStorage.saveAmoledSurfacesEnabled(false)
        }
    }

    fun setAmoledSurfaces(enabled: Boolean) {
        ensureLoaded()
        if (_amoledSurfacesEnabled.value == enabled) return
        _amoledSurfacesEnabled.value = enabled
        ThemeSettingsStorage.saveAmoledSurfacesEnabled(enabled)
    }

    fun setLiquidGlassNativeTabBar(enabled: Boolean) {
        ensureLoaded()
        if (_liquidGlassNativeTabBarEnabled.value == enabled) return
        _liquidGlassNativeTabBarEnabled.value = enabled
        ThemeSettingsStorage.saveLiquidGlassNativeTabBarEnabled(enabled)
        NativeTabBridge.publishLiquidGlassEnabled(enabled)
    }

    fun setGlassNavBar(enabled: Boolean) {
        ensureLoaded()
        if (_glassNavBarEnabled.value == enabled) return
        _glassNavBarEnabled.value = enabled
        ThemeSettingsStorage.saveGlassNavBarEnabled(enabled)
    }

    fun setAppLanguage(language: AppLanguage) {
        ensureLoaded()
        if (_selectedAppLanguage.value == language) return
        ThemeSettingsStorage.saveSelectedAppLanguage(language.code)
        ThemeSettingsStorage.applySelectedAppLanguage(language.code)
        _selectedAppLanguage.value = language
    }

    fun setDateFormatOption(format: DateFormatOption) {
        ensureLoaded()
        if (_dateFormatOption.value == format) return
        _dateFormatOption.value = format
        ThemeSettingsStorage.saveDateFormatOption(format.name)
    }
}

private fun nativeTabAccentHex(theme: AppTheme, customHex: String): String =
    ThemeColors.getColorPalette(theme).nativeAccentHex
