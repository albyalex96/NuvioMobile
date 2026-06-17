package com.nuvio.app.features.player.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.WindowState
import java.awt.Window
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame

private const val NuvioWindowBackgroundRgb = 0x0D0D0D
private const val NuvioWindowTextRgb = 0xF5F7F8

internal fun applyNativeDesktopWindowChrome(window: Window) {
    if (DesktopHostOs.current != DesktopHostOs.WINDOWS || !window.isDisplayable) return
    runCatching {
        val hwnd = AwtNativeViewResolver.resolveNativeViewPointer(window)
        NativePlayerBridge.applyWindowChrome(
            windowHwnd = hwnd,
            darkMode = true,
            captionColorRgb = NuvioWindowBackgroundRgb,
            borderColorRgb = NuvioWindowBackgroundRgb,
            textColorRgb = NuvioWindowTextRgb,
        )
    }
}

internal data class DesktopAppWindowChromeState(
    val isFullscreen: Boolean = false,
)

internal class DesktopAppFullscreenController {
    private var wasUndecorated = false

    fun toggle(window: ComposeWindow, windowState: WindowState) {
        if (window.extendedState == JFrame.MAXIMIZED_BOTH) {
            window.extendedState = JFrame.NORMAL
            window.isUndecorated = wasUndecorated
        } else {
            wasUndecorated = window.isUndecorated
            window.isUndecorated = true
            window.extendedState = JFrame.MAXIMIZED_BOTH
        }
    }

    fun dispose(window: ComposeWindow) {
        window.extendedState = JFrame.NORMAL
        window.isUndecorated = wasUndecorated
    }
}

internal fun installDesktopAppFullscreenShortcuts(window: ComposeWindow): () -> Unit {
    val actionMapKey = "toggle-fullscreen"
    val keyStroke = javax.swing.KeyStroke.getKeyStroke(
        java.awt.event.KeyEvent.VK_F,
        java.awt.event.InputEvent.CTRL_DOWN_MASK,
    )
    window.rootPane.inputMap.put(keyStroke, actionMapKey)
    window.rootPane.actionMap.put(actionMapKey, object : javax.swing.AbstractAction() {
        override fun actionPerformed(e: java.awt.event.ActionEvent?) {
            notifyDesktopAppFullscreenToggle(window)
        }
    })
    return {
        window.rootPane.inputMap.remove(keyStroke)
        window.rootPane.actionMap.remove(actionMapKey)
    }
}

private val fullscreenToggleListeners = mutableListOf<(ComposeWindow?) -> Unit>()

internal fun registerDesktopAppFullscreenToggle(listener: (ComposeWindow?) -> Unit): () -> Unit {
    fullscreenToggleListeners.add(listener)
    return { fullscreenToggleListeners.remove(listener) }
}

internal fun notifyDesktopAppFullscreenToggle(targetWindow: ComposeWindow?) {
    fullscreenToggleListeners.forEach { it(targetWindow) }
}
