package com.nuvio.app.core.ui

internal actual fun isLiquidGlassNativeTabBarSupported(): Boolean = false

internal actual fun publishLiquidGlassNativeTabBarEnabled(enabled: Boolean) {}

internal actual fun publishNativeTabBarVisible(visible: Boolean) {}

internal actual fun publishNativeSelectedTab(tabName: String) {}

internal actual fun publishNativeTabAccentColor(hexColor: String) {}

internal actual fun publishNativeProfileTabIcon(
    name: String?,
    avatarColorHex: String?,
    avatarImageUrl: String?,
    avatarBackgroundColorHex: String?,
) {}
