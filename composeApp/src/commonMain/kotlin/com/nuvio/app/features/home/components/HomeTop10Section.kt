package com.nuvio.app.features.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.stableKey
@Composable
fun HomeTop10Section(
    title: String,
    items: List<MetaPreview>,
    modifier: Modifier = Modifier,
    sectionPadding: Dp = 16.dp,
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
) {
    if (items.isEmpty()) return

    Text(
      text = title,
      modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = sectionPadding)
          .padding(bottom = 8.dp),
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onBackground,
      fontWeight = FontWeight.Bold,
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = sectionPadding),
    ) {
        itemsIndexed(items, key = { _, item -> item.stableKey() }) { index, item ->
            Top10PosterItem(
                item = item,
                rank = index + 1,
                onClick = onPosterClick?.let { { it(item) } },
                onLongClick = onPosterLongClick?.let { { it(item) } },
            )
        }
    }
}

@Composable
private fun Top10PosterItem(
    item: MetaPreview,
    rank: Int,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
) {
    // Each card is a poster with a large rank number overlapping on the left
    Box(
        modifier = Modifier
            .width(TOP10_ITEM_TOTAL_WIDTH.dp)
            .padding(end = 4.dp),
    ) {
        // Rank number — large, left-aligned, partially behind the poster
        Text(
            text = rank.toString(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 2.dp),
            fontSize = 80.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            color = Color.White,
            textAlign = TextAlign.Left,
            lineHeight = 80.sp,
        )

        // Poster card shifted right to overlap the number
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(TOP10_POSTER_WIDTH.dp),
        ) {
            HomePosterCard(
                item = item,
                onClick = onClick,
                onLongClick = onLongClick,
            )
        }
    }
}

private const val TOP10_POSTER_WIDTH = 110
private const val TOP10_RANK_WIDTH = 60
private const val TOP10_ITEM_TOTAL_WIDTH = TOP10_POSTER_WIDTH + TOP10_RANK_WIDTH