package com.nuvio.app.core.ui

import com.nuvio.app.core.storage.DesktopStorage

internal actual object CardDepthStyleStorage {
    private const val preferencesName = "nuvio_card_depth_style"
    private val store = DesktopStorage.store(preferencesName)

    actual fun loadPayload(): String? =
        store.getString("card_depth_style_payload")

    actual fun savePayload(payload: String) {
        store.putString("card_depth_style_payload", payload)
    }
}
