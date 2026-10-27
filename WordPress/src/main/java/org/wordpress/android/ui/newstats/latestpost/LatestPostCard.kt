package org.wordpress.android.ui.newstats.latestpost

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.newstats.components.CardPosition
import org.wordpress.android.ui.newstats.components.StatsBarChart
import org.wordpress.android.ui.newstats.components.StatsCardContainer
import org.wordpress.android.ui.newstats.components.StatsCardErrorContent
import org.wordpress.android.ui.newstats.components.StatsCardHeader
import org.wordpress.android.ui.newstats.components.StatsLabeledValue
import org.wordpress.android.ui.newstats.util.ShimmerBox

private val CardPadding = 16.dp
// Matches the featured image on the old Latest Post Summary card.
private val FeaturedImageSize = 68.dp
private val FeaturedImageCorner = 8.dp
private val ChartHeight = 48.dp

@Composable
@Suppress("LongParameterList")
fun LatestPostCard(
    uiState: LatestPostCardUiState,
    onRemoveCard: () -> Unit,
    onRetry: () -> Unit,
    onPostClick: (postId: Long, title: String) -> Unit,
    onCreatePostClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardPosition: CardPosition? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveToTop: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onMoveToBottom: (() -> Unit)? = null
) {
    StatsCardContainer(modifier = modifier) {
        when (uiState) {
            is LatestPostCardUiState.Loading ->
                LoadingContent()
            is LatestPostCardUiState.NoData ->
                NoDataContent(
                    onCreatePostClick,
                    onRemoveCard,
                    cardPosition,
                    onMoveUp,
                    onMoveToTop,
                    onMoveDown,
                    onMoveToBottom
                )
            is LatestPostCardUiState.Loaded ->
                LoadedContent(
                    uiState,
                    onPostClick,
                    onRemoveCard,
                    cardPosition,
                    onMoveUp,
                    onMoveToTop,
                    onMoveDown,
                    onMoveToBottom
                )
            LatestPostCardUiState.Error ->
                StatsCardErrorContent(
                    titleResId = R.string
                        .stats_insights_latest_post_summary,
                    errorMessageResId =
                        R.string.stats_error_api,
                    onRetry = onRetry,
                    onRemoveCard = onRemoveCard,
                    cardPosition = cardPosition,
                    onMoveUp = onMoveUp,
                    onMoveToTop = onMoveToTop,
                    onMoveDown = onMoveDown,
                    onMoveToBottom = onMoveToBottom
                )
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CardPadding)
    ) {
        ShimmerBar(width = 180.dp, height = 24.dp)
        Spacer(modifier = Modifier.height(20.dp))
        ShimmerBar(width = 240.dp, height = 20.dp)
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerBar(width = 100.dp, height = 14.dp)
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            repeat(STAT_COLUMN_COUNT) {
                ShimmerBar(width = 60.dp, height = 40.dp)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        ShimmerBar(
            width = 240.dp,
            height = ChartHeight
        )
    }
}

@Composable
private fun ShimmerBar(width: Dp, height: Dp) {
    ShimmerBox(
        modifier = Modifier
            .width(width)
            .height(height)
    )
}

@Suppress("LongParameterList")
@Composable
private fun NoDataContent(
    onCreatePostClick: () -> Unit,
    onRemoveCard: () -> Unit,
    cardPosition: CardPosition?,
    onMoveUp: (() -> Unit)?,
    onMoveToTop: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onMoveToBottom: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CardPadding)
    ) {
        StatsCardHeader(
            titleResId = R.string
                .stats_insights_latest_post_summary,
            onRemoveCard = onRemoveCard,
            cardPosition = cardPosition,
            onMoveUp = onMoveUp,
            onMoveToTop = onMoveToTop,
            onMoveDown = onMoveDown,
            onMoveToBottom = onMoveToBottom
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(
                R.string.stats_insights_latest_post_empty
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onCreatePostClick) {
            Text(
                text = stringResource(
                    R.string.stats_insights_create_post
                )
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun LoadedContent(
    state: LatestPostCardUiState.Loaded,
    onPostClick: (postId: Long, title: String) -> Unit,
    onRemoveCard: () -> Unit,
    cardPosition: CardPosition?,
    onMoveUp: (() -> Unit)?,
    onMoveToTop: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onMoveToBottom: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onPostClick(
                    state.postId,
                    state.postTitle
                )
            }
            .padding(CardPadding)
    ) {
        StatsCardHeader(
            titleResId = R.string
                .stats_insights_latest_post_summary,
            onRemoveCard = onRemoveCard,
            cardPosition = cardPosition,
            onMoveUp = onMoveUp,
            onMoveToTop = onMoveToTop,
            onMoveDown = onMoveDown,
            onMoveToBottom = onMoveToBottom
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.postTitle,
                    style = MaterialTheme.typography
                        .titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme
                        .onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = state.postDate,
                    style = MaterialTheme.typography
                        .bodySmall,
                    color = MaterialTheme.colorScheme
                        .onSurfaceVariant
                )
            }
            if (state.featuredImageUrl != null) {
                Spacer(modifier = Modifier.width(12.dp))
                AsyncImage(
                    model = state.featuredImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(FeaturedImageSize)
                        .clip(
                            RoundedCornerShape(
                                FeaturedImageCorner
                            )
                        )
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatsLabeledValue(
                labelResId = R.string.stats_views,
                value = state.views,
                modifier = Modifier.weight(1f)
            )
            StatsLabeledValue(
                labelResId = R.string.stats_likes,
                value = state.likes,
                modifier = Modifier.weight(1f)
            )
            StatsLabeledValue(
                labelResId = R.string.stats_comments,
                value = state.comments,
                modifier = Modifier.weight(1f)
            )
        }
        if (state.recentViews.any { it > 0L }) {
            Spacer(modifier = Modifier.height(16.dp))
            StatsBarChart(
                values = state.recentViews,
                height = ChartHeight,
                barSpacing = 4.dp
            )
        }
    }
}

private const val STAT_COLUMN_COUNT = 3

@Preview(showBackground = true)
@Composable
private fun LatestPostCardLoadedPreview() {
    AppThemeM3 {
        LatestPostCard(
            uiState = LatestPostCardUiState.Loaded(
                postId = 2729L,
                postTitle = "Ten things I learned " +
                    "building a birdhouse",
                postDate = "Aug 4, 2026",
                views = 4600L,
                likes = 32L,
                comments = 7L,
                recentViews = listOf(
                    12L, 40L, 33L, 80L, 65L, 21L, 54L
                ),
                featuredImageUrl = null
            ),
            onRemoveCard = {},
            onRetry = {},
            onPostClick = { _, _ -> },
            onCreatePostClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LatestPostCardErrorPreview() {
    AppThemeM3 {
        LatestPostCard(
            uiState = LatestPostCardUiState.Error,
            onRemoveCard = {},
            onRetry = {},
            onPostClick = { _, _ -> },
            onCreatePostClick = {}
        )
    }
}
