package com.nuvio.app

class WebPlatform : Platform {
    override val name: String = "Web"
}

actual fun getPlatform(): Platform = WebPlatform()

internal actual val isIos: Boolean = false
