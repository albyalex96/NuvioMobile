package com.nuvio.app.features.library

import com.nuvio.app.WebStorage

internal actual object LibraryStorage {
    actual fun loadPayload(profileId: Int): String? = WebStorage.getString("nuvio_library_$profileId")
    actual fun savePayload(profileId: Int, payload: String) { WebStorage.setString("nuvio_library_$profileId", payload) }
}
