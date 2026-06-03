package com.nuvio.app.features.updater

actual object AppUpdaterPlatform {
    actual val isSupported: Boolean = false

    actual fun getSupportedAbis(): List<String> = emptyList()
    actual fun getIgnoredTag(): String? = null
    actual fun setIgnoredTag(tag: String?) {}
    actual suspend fun downloadApk(
        assetUrl: String,
        assetName: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
    ): Result<String> = Result.failure(Exception("App updates not supported on web"))
    actual fun canRequestPackageInstalls(): Boolean = false
    actual fun openUnknownSourcesSettings() {}
    actual fun installDownloadedApk(path: String): Result<Unit> = Result.failure(Exception("App updates not supported on web"))
}
