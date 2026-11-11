package org.wordpress.android.ui.newstats.mostpopularday

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun MostPopularDayCard(
    uiState: MostPopularDayCardUiState,
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
            is MostPopularDayCardUiState.Loading ->
                LoadingContent()
            is MostPopularDayCardUiState.NoData ->
                NoDataContent(
                    onRemoveCard,
                    cardPosition,
                    onMoveUp,
                    onMoveToTop,
                    onMoveDown,
                    onMoveToBottom
                )
            is MostPopularDayCardUiState.Loaded ->
                LoadedContent(
                    uiState,
                    onRemoveCard,
                    cardPosition,
                    onMoveUp,
                    onMoveToTop,
                    onMoveDown,
                    onMoveToBottom
                )
            is MostPopularDayCardUiState.Error ->
                StatsCardErrorContent(
                    titleResId = R.string
                        .stats_insights_most_popular_day,
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
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun NoDataContent(
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
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    R.string
                        .stats_insights_most_popular_day
                ),
                style = MaterialTheme.typography
                    .titleMedium,
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
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(
                R.string.stats_no_data_yet
            ),
            style = MaterialTheme.typography
                .bodyMedium,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun LoadedContent(
    state: MostPopularDayCardUiState.Loaded,
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
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    R.string
                        .stats_insights_most_popular_day
                ),
                style = MaterialTheme.typography
                    .titleMedium,
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
        Spacer(modifier = Modifier.height(12.dp))
        // Day section
        Text(
            text = stringResource(
                R.string
                    .stats_insights_most_popular_day_label
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = state.dayAndMonth,
            style = MaterialTheme.typography
                .headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (state.year.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = state.year,
                style = MaterialTheme.typography
                    .bodyMedium,
                color = MaterialTheme.colorScheme
                    .onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Views section
        Text(
            text = stringResource(R.string.stats_views),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatStatValue(state.views),
            style = MaterialTheme.typography
                .headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(
                R.string
                    .stats_insights_views_percent,
                state.viewsPercentage
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MostPopularDayCardLoadingPreview() {
    AppThemeM3 {
        MostPopularDayCard(
            uiState =
                MostPopularDayCardUiState.Loading,
            onRemoveCard = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MostPopularDayCardLoadedPreview() {
    AppThemeM3 {
        MostPopularDayCard(
            uiState =
                MostPopularDayCardUiState.Loaded(
                    dayAndMonth = "February 22",
                    year = "2022",
                    views = 4600L,
                    viewsPercentage = "0.068"
                ),
            onRemoveCard = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MostPopularDayCardErrorPreview() {
    AppThemeM3 {
        MostPopularDayCard(
            uiState = MostPopularDayCardUiState.Error(
                message = "Failed to load stats"
            ),
            onRemoveCard = {},
            onRetry = {}
        )
    }
}
