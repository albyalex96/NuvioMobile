package com.nuvio.app.features.downloads

data class HlsVariant(
    val bandwidth: Long,
    val resolution: String? = null,
    val codecs: String? = null,
    val url: String,
)

data class HlsMediaTrack(
    val type: String,
    val groupId: String,
    val name: String,
    val language: String? = null,
    val uri: String? = null,
    val isDefault: Boolean = false,
    val isForced: Boolean = false,
)

data class HlsMasterPlaylist(
    val variants: List<HlsVariant>,
    val audioTracks: List<HlsMediaTrack>,
    val subtitleTracks: List<HlsMediaTrack>,
)

data class HlsKeyInfo(
    val method: String,
    val uri: String,
    val iv: String? = null,
)

data class HlsMapInfo(
    val uri: String,
    val byteRange: String? = null,
)

data class HlsMediaPlaylist(
    val segments: List<HlsSegment>,
    val targetDuration: Double = 0.0,
    val keys: List<HlsKeyInfo> = emptyList(),
    val map: HlsMapInfo? = null,
)

data class HlsSegment(
    val duration: Double,
    val url: String,
    val keyIndex: Int? = null,
)

object HlsPlaylistParser {

    fun isHlsUrl(url: String): Boolean {
        val lower = url.trim().lowercase()
        return lower.endsWith(".m3u8") ||
            lower.contains(".m3u8?") ||
            lower.contains("/playlist/") ||
            lower.contains("/master/") ||
            lower.contains("/chunklist/")
    }

    fun isHlsStream(streamType: String?): Boolean =
        streamType?.trim().equals("hls", ignoreCase = true)

    fun isHlsContentType(contentType: String?): Boolean {
        val ct = contentType?.trim().orEmpty().lowercase()
        return ct.contains("vnd.apple.mpegurl") ||
            ct.contains("mpegurl") ||
            ct.contains("x-mpegurl")
    }

    fun parseMasterPlaylist(content: String, baseUrl: String): HlsMasterPlaylist {
        val lines = content.lines()
        val variants = mutableListOf<HlsVariant>()
        val audioTracks = mutableListOf<HlsMediaTrack>()
        val subtitleTracks = mutableListOf<HlsMediaTrack>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            when {
                line.startsWith("#EXT-X-STREAM-INF:") -> {
                    val attrs = parseAttributes(line.removePrefix("#EXT-X-STREAM-INF:"))
                    val bandwidth = attrs["BANDWIDTH"]?.toLongOrNull() ?: 0L
                    val resolution = attrs["RESOLUTION"]
                    val codecs = attrs["CODECS"]
                    i++
                    val urlLine = resolveUrl(lines.getOrNull(i)?.trim().orEmpty(), baseUrl)
                    if (urlLine.isNotBlank() && !urlLine.startsWith("#")) {
                        variants.add(HlsVariant(bandwidth, resolution, codecs, urlLine))
                    }
                }
                line.startsWith("#EXT-X-MEDIA:") -> {
                    val attrs = parseAttributes(line.removePrefix("#EXT-X-MEDIA:"))
                    val type = attrs["TYPE"]?.trim()?.uppercase() ?: ""
                    val groupId = attrs["GROUP-ID"]?.trim() ?: ""
                    val name = removeQuotes(attrs["NAME"]?.trim().orEmpty())
                    val language = attrs["LANGUAGE"]?.trim()?.let { removeQuotes(it) }
                    val uri = attrs["URI"]?.trim()?.let { resolveUrl(removeQuotes(it), baseUrl) }
                    val isDefault = attrs["DEFAULT"]?.trim()?.uppercase() == "YES"
                    val isForced = attrs["FORCED"]?.trim()?.uppercase() == "YES"

                    val track = HlsMediaTrack(
                        type = type,
                        groupId = groupId,
                        name = name,
                        language = language,
                        uri = uri,
                        isDefault = isDefault,
                        isForced = isForced,
                    )
                    when (type) {
                        "AUDIO" -> audioTracks.add(track)
                        "SUBTITLES" -> subtitleTracks.add(track)
                    }
                }
            }
            i++
        }

        return HlsMasterPlaylist(variants, audioTracks, subtitleTracks)
    }

    fun parseMediaPlaylist(content: String, baseUrl: String): HlsMediaPlaylist {
        val lines = content.lines()
        val segments = mutableListOf<HlsSegment>()
        val keys = mutableListOf<HlsKeyInfo>()
        var targetDuration = 0.0
        var currentDuration = 0.0
        var currentKeyIndex: Int? = null
        var map: HlsMapInfo? = null

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#EXT-X-KEY:") -> {
                    val attrs = parseAttributes(trimmed.removePrefix("#EXT-X-KEY:"))
                    val method = attrs["METHOD"]?.trim()?.uppercase() ?: "NONE"
                    if (method == "NONE") {
                        currentKeyIndex = null
                    } else {
                        val keyUri = attrs["URI"]?.let { resolveUrl(removeQuotes(it.trim()), baseUrl) }.orEmpty()
                        val keyIv = attrs["IV"]?.trim()
                        keys.add(HlsKeyInfo(method, keyUri, keyIv))
                        currentKeyIndex = keys.size - 1
                    }
                }
                trimmed.startsWith("#EXT-X-MAP:") -> {
                    val attrs = parseAttributes(trimmed.removePrefix("#EXT-X-MAP:"))
                    val mapUri = attrs["URI"]?.let { resolveUrl(removeQuotes(it.trim()), baseUrl) }.orEmpty()
                    val byteRange = attrs["BYTERANGE"]?.trim()
                    map = HlsMapInfo(mapUri, byteRange)
                }
                trimmed.startsWith("#EXT-X-TARGETDURATION:") -> {
                    targetDuration = trimmed.substringAfter(":").trim().toDoubleOrNull() ?: 0.0
                }
                trimmed.startsWith("#EXTINF:") -> {
                    val durationStr = trimmed.substringAfter(":").substringBefore(",").trim()
                    currentDuration = durationStr.toDoubleOrNull() ?: 0.0
                }
                !trimmed.startsWith("#") && trimmed.isNotBlank() -> {
                    val segmentUrl = resolveUrl(trimmed, baseUrl)
                    segments.add(HlsSegment(currentDuration, segmentUrl, currentKeyIndex))
                    currentDuration = 0.0
                }
            }
        }

        return HlsMediaPlaylist(segments, targetDuration, keys, map)
    }

    fun deriveIv(keyInfo: HlsKeyInfo, sequenceIndex: Int): ByteArray {
        keyInfo.iv?.let { ivHex ->
            val hex = ivHex.trimStart('0', 'x')
            return hexStringToBytes(hex.ifEmpty { "00" })
        }
        val iv = ByteArray(16)
        var idx = sequenceIndex
        for (i in 15 downTo 0) {
            iv[i] = (idx and 0xFF).toByte()
            idx = idx ushr 8
        }
        return iv
    }

    private fun hexStringToBytes(hex: String): ByteArray {
        val normalized = hex.ifEmpty { "0" }
        val padded = if (normalized.length % 2 != 0) "0$normalized" else normalized
        return ByteArray(padded.length / 2) {
            padded.substring(it * 2, it * 2 + 2).toInt(16).toByte()
        }
    }

    val HlsMediaPlaylist.isFmp4: Boolean get() = map != null

    private fun parseAttributes(input: String): Map<String, String> {
        val attrs = mutableMapOf<String, String>()
        var i = 0
        val len = input.length
        while (i < len) {
            while (i < len && input[i].isWhitespace()) i++
            if (i >= len) break
            val eqIdx = input.indexOf('=', i)
            if (eqIdx == -1) break
            val key = input.substring(i, eqIdx).trim()
            i = eqIdx + 1
            if (i >= len) break
            val value: String
            if (input[i] == '"') {
                i++
                val closeIdx = input.indexOf('"', i)
                if (closeIdx == -1) {
                    value = input.substring(i)
                    i = len
                } else {
                    value = input.substring(i, closeIdx)
                    i = closeIdx + 1
                }
            } else {
                val nextComma = input.indexOf(',', i)
                if (nextComma == -1) {
                    value = input.substring(i).trim()
                    i = len
                } else {
                    value = input.substring(i, nextComma).trim()
                    i = nextComma + 1
                }
            }
            if (key.isNotBlank()) {
                attrs[key] = value
            }
        }
        return attrs
    }

    private fun removeQuotes(value: String): String =
        value.trim().removeSurrounding("\"")

    fun resolveUrl(relative: String, baseUrl: String): String {
        val trimmed = relative.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        if (trimmed.isBlank()) return trimmed
        val base = baseUrl.trimEnd('/')
        val basePath = if (base.contains('/')) {
            base.substringBeforeLast('/')
        } else {
            base
        }
        return "$basePath/$trimmed"
    }
}
