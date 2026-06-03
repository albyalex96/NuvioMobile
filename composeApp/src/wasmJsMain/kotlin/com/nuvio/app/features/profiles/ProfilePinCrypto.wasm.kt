package com.nuvio.app.features.profiles

internal actual object ProfilePinCrypto {
    actual fun sha256Hex(value: String): String {
        return value.hashCode().toUInt().toString(16)
    }
}
