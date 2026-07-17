package com.nuvio.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.settings.NuvioAppIconSwitcher
import com.nuvio.app.features.settings.ThemeSettingsStorage
import com.nuvio.app.features.player.PlatformPlayerSurface
import com.nuvio.app.features.player.desktop.DesktopAppFullscreenController
import com.nuvio.app.features.player.desktop.applyNativeDesktopWindowChrome
import com.nuvio.app.features.player.desktop.installDesktopAppFullscreenShortcuts
import com.nuvio.app.features.player.desktop.preloadNativePlayerBridgeAsync
import com.nuvio.app.features.player.desktop.registerDesktopAppFullscreenToggle
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.app_icon_aurora_preview
import nuvio.composeapp.generated.resources.app_icon_chrome_preview
import nuvio.composeapp.generated.resources.app_icon_default_preview
import nuvio.composeapp.generated.resources.app_icon_emerald_preview
import nuvio.composeapp.generated.resources.app_icon_enhanced_preview
import nuvio.composeapp.generated.resources.app_icon_gear_preview
import nuvio.composeapp.generated.resources.app_icon_monochrome_preview
import nuvio.composeapp.generated.resources.app_icon_neon_preview
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import java.awt.Color as AwtColor
import javax.swing.JComponent

private val NuvioDesktopNativeBackground = AwtColor(0x0D, 0x0D, 0x0D)
private const val MacosDarkAquaAppearance = "NSAppearanceNameDarkAqua"

fun main() {
    configureDesktopChrome()
    preloadNativePlayerBridgeAsync()

    application {
        val smokePlayerUrl = (
            System.getProperty("nuvio.desktop.smokePlayerUrl")
                ?: System.getenv("NUVIO_DESKTOP_SMOKE_PLAYER_URL")
            )
            ?.takeIf { it.isNotBlank() }
        val windowState = rememberWindowState(width = 1280.dp, height = 820.dp)
        val fullscreenController = remember { DesktopAppFullscreenController() }

        val savedIconId = remember { ThemeSettingsStorage.loadSelectedAppIconId() }
        if (savedIconId != null) {
            NuvioAppIconSwitcher.currentIconId = savedIconId
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = if (smokePlayerUrl == null) "Nuvio" else "Nuvio Player Smoke",
            state = windowState,
            icon = iconPainterFor(NuvioAppIconSwitcher.currentIconId),
        ) {
            SideEffect {
                window.background = NuvioDesktopNativeBackground
                window.rootPane.background = NuvioDesktopNativeBackground
                window.contentPane.background = NuvioDesktopNativeBackground
                (window.contentPane as? JComponent)?.isOpaque = true
            }
            LaunchedEffect(window) {
                applyNativeDesktopWindowChrome(window)
            }
            DisposableEffect(window, windowState) {
                val unregisterFullscreenToggle = registerDesktopAppFullscreenToggle { targetWindow ->
                    if (targetWindow != null && targetWindow !== window) return@registerDesktopAppFullscreenToggle
                    fullscreenController.toggle(window, windowState)
                }
                val uninstallFullscreenShortcuts = installDesktopAppFullscreenShortcuts(window)
                onDispose {
                    fullscreenController.dispose(window)
                    uninstallFullscreenShortcuts()
                    unregisterFullscreenToggle()
                }
            }

            if (smokePlayerUrl == null) {
                App()
            } else {
                PlatformPlayerSurface(
                    sourceUrl = smokePlayerUrl,
                    modifier = Modifier.fillMaxSize(),
                    onControllerReady = {},
                    onSnapshot = {},
                    onError = {},
                )
            }
        }
    }
}

private fun configureDesktopChrome() {
    if (System.getProperty("os.name").contains("mac", ignoreCase = true)) {
        System.setProperty("apple.awt.application.appearance", MacosDarkAquaAppearance)
    }
}

@Composable
private fun iconPainterFor(iconId: String): Painter? {
    val resource: DrawableResource? = when (iconId) {
        "default" -> Res.drawable.app_icon_default_preview
        "enhanced" -> Res.drawable.app_icon_enhanced_preview
        "monochrome" -> Res.drawable.app_icon_monochrome_preview
        "neon" -> Res.drawable.app_icon_neon_preview
        "gear" -> Res.drawable.app_icon_gear_preview
        "chrome" -> Res.drawable.app_icon_chrome_preview
        "aurora" -> Res.drawable.app_icon_aurora_preview
        "emerald" -> Res.drawable.app_icon_emerald_preview
        else -> null
    }
    return resource?.let { painterResource(it) }
}
