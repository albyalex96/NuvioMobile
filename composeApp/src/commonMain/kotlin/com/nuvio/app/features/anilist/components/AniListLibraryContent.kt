package com.nuvio.app.features.anilist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioShelfSection
import com.nuvio.app.features.anilist.AniListLibraryUiState
import com.nuvio.app.features.anilist.AniListLibraryItem
import com.nuvio.app.features.anilist.AniListSortBy
import com.nuvio.app.features.home.components.HomeSkeletonRow
import com.nuvio.app.features.home.components.HomeEmptyStateCard

fun LazyListScope.aniListLibraryContent(
    uiState: AniListLibraryUiState,
    sectionsConfig: List<com.nuvio.app.features.anilist.AniListSectionSettings>,
    sortBy: AniListSortBy,
    sortAscending: Boolean,
    onPosterClick: (AniListLibraryItem) -> Unit,
    onEditClick: (AniListLibraryItem) -> Unit,
    onConnectAniListClick: () -> Unit,
    onRefresh: () -> Unit,
    isOffline: Boolean
) {
    when {
        uiState.isLoading && !uiState.isLoaded -> {
            items(3) {
                HomeSkeletonRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    showHeaderAccent = true
                )
            }
        }

        !uiState.isLoaded -> {
            item {
                HomeEmptyStateCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = "AniList Not Connected",
                    message = "Connect your AniList account in Settings to view your custom anime shelves here.",
                    actionLabel = "Connect Now",
                    onActionClick = onConnectAniListClick
                )
            }
        }

        isOffline -> {
            item {
                HomeEmptyStateCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = "You are Offline",
                    message = "Internet connection is required to view your AniList library shelves.",
                    actionLabel = "Retry",
                    onActionClick = onRefresh
                )
            }
        }

        else -> {
            val sections = sectionsConfig.mapNotNull { sectionConfig ->
                if (!sectionConfig.enabled) return@mapNotNull null
                val rawList = when (sectionConfig.type) {
                    "Watching" -> uiState.watching
                    "Completed" -> uiState.completed
                    "Planning" -> uiState.planning
                    "Paused" -> uiState.paused
                    "Dropped" -> uiState.dropped
                    "Rewatching" -> uiState.rewatching
                    else -> emptyList()
                }
                val sorted = rawList.sortedWith(
                    when (sortBy) {
                        AniListSortBy.LAST_UPDATED -> compareBy { it.updatedAt }
                        AniListSortBy.SCORE -> compareBy { it.score ?: 0 }
                        AniListSortBy.TITLE -> compareBy { it.title.lowercase() }
                        AniListSortBy.RELEASE_DATE -> compareBy { it.updatedAt } // updatedAt as proxy
                    }
                ).let { if (sortAscending) it else it.reversed() }
                Pair(sectionConfig.type, sorted)
            }

            var displayedAnySection = false

            sections.forEach { (title, list) ->
                if (list.isNotEmpty()) {
                    displayedAnySection = true
                    item {
                        NuvioShelfSection(
                            title = title,
                            entries = list,
                            headerHorizontalPadding = 16.dp,
                            rowContentPadding = PaddingValues(horizontal = 16.dp),
                            key = { entry -> entry.id }
                        ) { entry ->
                            AniListPosterCard(
                                item = entry,
                                onClick = { onPosterClick(entry) },
                                onEditClick = { onEditClick(entry) }
                            )
                        }
                    }
                }
            }

            if (!displayedAnySection) {
                item {
                    HomeEmptyStateCard(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        title = "Your Lists are Empty",
                        message = "Start watching or planning anime on AniList to see them show up here.",
                        actionLabel = "Refresh Now",
                        onActionClick = onRefresh
                    )
                }
            }
        }
    }
}
