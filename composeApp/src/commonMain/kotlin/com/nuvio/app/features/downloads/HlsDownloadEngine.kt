package com.nuvio.app.features.downloads

internal class HlsHttpResult(
    val status: Int,
    val body: ByteArray,
    val finalUrl: String,
)

internal class HlsDownloadOutcome(
    val isFmp4: Boolean,
    val totalBytes: Long,
)

internal class HlsDownloadException(message: String) : Exception(message)

private const val MAX_PLAYLIST_REDIRECTS = 4
private const val AES_128 = "AES-128"
private const val BITRATE_WARMUP_FRACTION = 0.08

private class ResolvedHlsMedia(
    val playlist: HlsMediaPlaylist,
    val declaredBitsPerSecond: Long?,
)

internal suspend fun downloadHlsToFile(
    sourceUrl: String,
    httpGet: suspend (url: String, range: HlsByteRange?) -> HlsHttpResult,
    appendBytes: (ByteArray) -> Unit,
    decryptAes128Cbc: (data: ByteArray, key: ByteArray, iv: ByteArray) -> ByteArray,
    onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
    ensureActive: () -> Unit,
): HlsDownloadOutcome {
    val resolved = resolveMediaPlaylist(sourceUrl, httpGet, ensureActive)
    val media = resolved.playlist
    val segments = media.segments
    if (segments.isEmpty()) {
        throw HlsDownloadException("HLS playlist contained no segments")
    }

    onProgress(0L, null)
    var cumulativeBytes = 0L
    val keyCache = HashMap<String, ByteArray>()

    media.initSection?.let { init ->
        ensureActive()
        val response = httpGet(init.url, init.byteRange)
        if (response.status !in 200..299) {
            throw HlsDownloadException("HLS init section failed (HTTP ${response.status})")
        }
        appendBytes(response.body)
        cumulativeBytes += response.body.size.toLong()
    }

    val totalDuration = segments.sumOf { it.durationSeconds }
    val warmupDuration = totalDuration * BITRATE_WARMUP_FRACTION
    var totalBytesEstimate: Long? = declaredTotalBytes(resolved.declaredBitsPerSecond, totalDuration)
    var elapsedDuration = 0.0
    val totalSegments = segments.size

    segments.forEachIndexed { index, segment ->
        ensureActive()
        val response = httpGet(segment.url, segment.byteRange)
        if (response.status !in 200..299) {
            throw HlsDownloadException(
                "HLS segment ${index + 1}/$totalSegments failed (HTTP ${response.status})",
            )
        }

        val payload = decryptSegmentIfNeeded(
            data = response.body,
            encryption = segment.encryption,
            sequenceNumber = segment.sequenceNumber,
            keyCache = keyCache,
            httpGet = httpGet,
            decryptAes128Cbc = decryptAes128Cbc,
            ensureActive = ensureActive,
        )

        appendBytes(payload)
        cumulativeBytes += payload.size.toLong()
        elapsedDuration += segment.durationSeconds

        if (totalBytesEstimate == null && totalDuration > 0.0 && elapsedDuration >= warmupDuration) {
            totalBytesEstimate = (cumulativeBytes.toDouble() / elapsedDuration * totalDuration).toLong()
        }
        onProgress(cumulativeBytes, totalBytesEstimate?.coerceAtLeast(cumulativeBytes))
    }

    return HlsDownloadOutcome(isFmp4 = media.isFmp4, totalBytes = cumulativeBytes)
}

private suspend fun decryptSegmentIfNeeded(
    data: ByteArray,
    encryption: HlsSegmentEncryption?,
    sequenceNumber: Long,
    keyCache: MutableMap<String, ByteArray>,
    httpGet: suspend (url: String, range: HlsByteRange?) -> HlsHttpResult,
    decryptAes128Cbc: (data: ByteArray, key: ByteArray, iv: ByteArray) -> ByteArray,
    ensureActive: () -> Unit,
): ByteArray {
    if (encryption == null) return data
    if (encryption.method != AES_128) {
        throw HlsDownloadException("Unsupported HLS encryption method: ${encryption.method}")
    }

    val key = keyCache.getOrPut(encryption.keyUrl) {
        ensureActive()
        val keyResponse = httpGet(encryption.keyUrl, null)
        if (keyResponse.status !in 200..299) {
            throw HlsDownloadException("HLS key fetch failed (HTTP ${keyResponse.status})")
        }
        if (keyResponse.body.size < 16) {
            throw HlsDownloadException("HLS key was not 16 bytes")
        }
        keyResponse.body.copyOf(16)
    }

    val iv = encryption.iv ?: hlsSequenceIv(sequenceNumber)
    return decryptAes128Cbc(data, key, iv)
}

private suspend fun resolveMediaPlaylist(
    sourceUrl: String,
    httpGet: suspend (url: String, range: HlsByteRange?) -> HlsHttpResult,
    ensureActive: () -> Unit,
): ResolvedHlsMedia {
    var currentUrl = sourceUrl
    var declaredBitsPerSecond: Long? = null
    repeat(MAX_PLAYLIST_REDIRECTS) {
        ensureActive()
        val response = httpGet(currentUrl, null)
        if (response.status !in 200..299) {
            throw HlsDownloadException("HLS playlist fetch failed (HTTP ${response.status})")
        }
        val baseUrl = response.finalUrl.ifBlank { currentUrl }
        when (val parsed = parseHlsPlaylist(response.body.decodeToString(), baseUrl)) {
            is HlsPlaylist.Media -> return ResolvedHlsMedia(parsed.playlist, declaredBitsPerSecond)
            is HlsPlaylist.Master -> {
                val variant = parsed.variants.maxByOrNull { it.bandwidth }
                    ?: throw HlsDownloadException("HLS master playlist contained no variants")
                declaredBitsPerSecond = (variant.averageBandwidth ?: variant.bandwidth)
                    .takeIf { it > 0L }
                currentUrl = variant.url
            }
        }
    }
    throw HlsDownloadException("HLS playlist nested too deeply")
}

internal fun declaredTotalBytes(bitsPerSecond: Long?, totalDuration: Double): Long? {
    if (bitsPerSecond == null || bitsPerSecond <= 0L || totalDuration <= 0.0) return null
    return (bitsPerSecond.toDouble() / 8.0 * totalDuration).toLong().takeIf { it > 0L }
}

internal fun hlsOutputFileName(baseFileName: String, isFmp4: Boolean): String {
    val extension = if (isFmp4) "mp4" else "ts"
    val dotIndex = baseFileName.lastIndexOf('.')
    val stem = if (dotIndex > 0) baseFileName.substring(0, dotIndex) else baseFileName
    return "$stem.$extension"
}

internal fun hlsCompanionFileName(baseFileName: String, suffix: String, extension: String): String {
    val dotIndex = baseFileName.lastIndexOf('.')
    val stem = if (dotIndex > 0) baseFileName.substring(0, dotIndex) else baseFileName
    return "${stem}_$suffix.$extension"
}
