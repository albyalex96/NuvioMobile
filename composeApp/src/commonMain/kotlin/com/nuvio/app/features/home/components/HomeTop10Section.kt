package com.nuvio.app.features.home.components

import androidx.compose.ui.graphics.StrokeJoin 
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioShelfSectionHeader
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.stableKey

data class SvgData(val path: String, val viewBoxWidth: Float, val viewBoxHeight: Float)

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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        NuvioShelfSectionHeader(
            title = title,
            modifier = Modifier.padding(horizontal = sectionPadding),
        )

        LazyRow(
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
}

@Composable
private fun Top10PosterItem(
    item: MetaPreview,
    rank: Int,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .width(TOP10_ITEM_TOTAL_WIDTH.dp)
            .height(TOP10_ITEM_HEIGHT.dp),
    ) {
        // Large rank number, vertically centered, anchored to the left
        Text(
            text = rank.toString(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 4.dp),
            fontSize = 110.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            color = Color(0xFF1A1A1A),
            lineHeight = 110.sp,
            style = TextStyle(
                drawStyle = Stroke(
                    width = 6f,
                    join = StrokeJoin.Round,
                )
),
        )

        // Poster overlapping the right half of the number
        Box(
            modifier = Modifier
                .width(TOP10_POSTER_WIDTH.dp)
                .fillMaxHeight()
                .align(Alignment.CenterEnd),
        ) {
            HomePosterCard(
                item = item,
                onClick = onClick,
                onLongClick = onLongClick,
            )
        }
    }
}

private const val TOP10_ITEM_HEIGHT = 160
private const val TOP10_POSTER_WIDTH = 107
private const val TOP10_RANK_WIDTH = 80
private const val TOP10_ITEM_TOTAL_WIDTH = TOP10_POSTER_WIDTH + TOP10_RANK_WIDTH