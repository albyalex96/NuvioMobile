package com.nuvio.app.features.mal

object MalIdResolver {
    private val cache = mutableMapOf<String, Long>()

    suspend fun resolve(
        contentId: String,
        name: String,
        releaseInfo: String?,
        mediaType: String,
    ): Long? {
        cache[contentId]?.let { return it }

        MalSyncRepository.extractMalId(contentId)?.let {
            cache[contentId] = it
            return it
        }

        if (!mediaType.equals("anime", ignoreCase = true)) return null

        val year = releaseInfo?.substringBefore('-')?.trim()?.toIntOrNull()
        val response = MalRepository.searchAnime(query = name, limit = 5)
        val bestMatch = findBestMatch(response, name, year)
        if (bestMatch != null) {
            cache[contentId] = bestMatch
        }
        return bestMatch
    }

    fun clearCache() {
        cache.clear()
    }

    private fun findBestMatch(
        response: MalSearchResponse,
        queryName: String,
        year: Int?,
    ): Long? {
        val results = response.data.mapNotNull { it.node }
        if (results.isEmpty()) return null

        val queryLower = queryName.trim().lowercase()

        data class Candidate(val id: Long, val score: Int)

        val candidates = results.map { anime ->
            val title = anime.title?.trim()?.lowercase() ?: ""
            var score = 0
            if (title == queryLower) {
                score += 100
            } else if (queryLower in title || title in queryLower) {
                score += 50
            } else {
                val queryTokens = queryLower.split(' ').filter { it.isNotBlank() }
                val titleTokens = title.split(' ').filter { it.isNotBlank() }
                val commonTokens = queryTokens.intersect(titleTokens.toSet()).size
                if (commonTokens > 0) score += commonTokens * 10
            }
            Candidate(anime.id ?: 0L, score)
        }

        return candidates.maxByOrNull { it.score }?.let {
            if (it.score > 0) it.id else null
        }
    }
}
