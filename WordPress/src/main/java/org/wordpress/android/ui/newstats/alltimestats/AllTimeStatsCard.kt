package org.wordpress.android.ui.newstats.alltimestats

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.newstats.components.CardPosition
import org.wordpress.android.ui.newstats.components.StatsCardContainer
import org.wordpress.android.ui.newstats.components.StatsCardErrorContent
import org.wordpress.android.ui.newstats.components.StatsCardMenu
import org.wordpress.android.ui.newstats.util.formatStatValue
import org.wordpress.android.ui.newstats.util.rememberShimmerBrush

private val CardPadding = 16.dp

@Composable
@Suppress("LongParameterList")
fun AllTimeStatsCard(
    uiState: AllTimeStatsCardUiState,
    onRemoveCard: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    cardPosition: CardPosition? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveToTop: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onMoveToBottom: (() -> Unit)? = null
) {
    StatsCardContainer(modifier = modifier) {
        when (uiState) {
            is AllTimeStatsCardUiState.Loading ->
                LoadingContent()
            is AllTimeStatsCardUiState.Loaded ->
                LoadedContent(
                    uiState,
                    onRemoveCard,
                    cardPosition,
                    onMoveUp,
                    onMoveToTop,
                    onMoveDown,
                    onMoveToBottom
                )
            is AllTimeStatsCardUiState.Error ->
                StatsCardErrorContent(
                    titleResId = R.string
                        .stats_insights_all_time_stats_title,
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
    val shimmerBrush = rememberShimmerBrush()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CardPadding)
    ) {
        // Title shimmer
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(16.dp))
        // 4 row shimmers
        repeat(4) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
            if (index < 3) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun LoadedContent(
    state: AllTimeStatsCardUiState.Loaded,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    R.string
                        .stats_insights_all_time_stats_title
                ),
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            StatsCardMenu(
                onRemoveClick = onRemoveCard,
                cardPosition = cardPosition,
                onMoveUp = onMoveUp,
                onMoveToTop = onMoveToTop,
                onMoveDown = onMoveDown,
                onMoveToBottom = onMoveToBottom
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        StatRow(
            icon = Icons.Outlined.Visibility,
            labelRes = R.string.stats_views,
            value = formatStatValue(state.views)
        )
        StatRow(
            icon = Icons.Outlined.People,
            labelRes = R.string.stats_visitors,
            value = formatStatValue(state.visitors)
        )
        StatRow(
            icon =
                Icons.AutoMirrored.Outlined.Article,
            labelRes = R.string.stats_insights_posts,
            value = formatStatValue(state.posts)
        )
        StatRow(
            icon =
                Icons.AutoMirrored.Outlined.Chat,
            labelRes = R.string.stats_comments,
            value = formatStatValue(state.comments)
        )
    }
}

@Composable
private fun StatRow(
    icon: ImageVector,
    @StringRes labelRes: Int,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme
                .onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AllTimeStatsCardLoadingPreview() {
    AppThemeM3 {
        AllTimeStatsCard(
            uiState = AllTimeStatsCardUiState.Loading,
            onRemoveCard = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AllTimeStatsCardLoadedPreview() {
    AppThemeM3 {
        AllTimeStatsCard(
            uiState = AllTimeStatsCardUiState.Loaded(
                views = 6782856L,
                visitors = 154791L,
                posts = 2L,
                comments = 0L
            ),
            onRemoveCard = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AllTimeStatsCardErrorPreview() {
    AppThemeM3 {
        AllTimeStatsCard(
            uiState = AllTimeStatsCardUiState.Error(
                message = "Failed to load stats"
            ),
            onRemoveCard = {},
            onRetry = {}
        )
    }
}
