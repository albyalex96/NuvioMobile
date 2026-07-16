package com.nuvio.app.features.cloudstream

internal actual object CloudStreamPlatformStorage {
    actual fun initialize(context: Any?) {}

    actual fun setActiveProfile(profileId: Int) {}

    actual fun loadState(profileId: Int): String? = null

    actual fun saveState(profileId: Int, payload: String) {}

    actual fun savePackageAtomically(storageKey: String, bytes: ByteArray) {}

    actual fun packageExists(storageKey: String): Boolean = false

    actual fun migratePackage(oldStorageKey: String, newStorageKey: String): Boolean = false

    actual fun packagePath(storageKey: String): String? = null

    actual fun deletePackage(storageKey: String) {}

    actual fun clearPackages() {}

    actual fun clearAllState() {}
}
