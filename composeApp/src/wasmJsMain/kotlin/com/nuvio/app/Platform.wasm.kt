package com.nuvio.app

import kotlinx.browser.window

class WasmPlatform : Platform {
    override val name: String = "WebAssembly"
}

actual fun getPlatform(): Platform = WasmPlatform()

internal actual val isIos: Boolean = false
