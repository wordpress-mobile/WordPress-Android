package org.wordpress.android.ui.newstats.mostpopulartime

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.newstats.components.CardPosition
import org.wordpress.android.ui.newstats.components.StatsCardContainer
import org.wordpress.android.ui.newstats.components.StatsCardErrorContent
import org.wordpress.android.ui.newstats.components.StatsCardHeader
import org.wordpress.android.ui.newstats.util.ShimmerBox

private val CardPadding = 16.dp

@Composable
@Suppress("LongParameterList")
fun MostPopularTimeCard(
    uiState: MostPopularTimeCardUiState,
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
            is MostPopularTimeCardUiState.Loading ->
                LoadingContent()
            is MostPopularTimeCardUiState.NoData ->
                NoDataContent(
                    onRemoveCard,
                    cardPosition,
                    onMoveUp,
                    onMoveToTop,
                    onMoveDown,
                    onMoveToBottom
                )
            is MostPopularTimeCardUiState.Loaded ->
                LoadedContent(
                    uiState,
                    onRemoveCard,
                    cardPosition,
                    onMoveUp,
                    onMoveToTop,
                    onMoveDown,
                    onMoveToBottom
                )
            is MostPopularTimeCardUiState.Error ->
                StatsCardErrorContent(
                    titleResId = R.string
                        .stats_insights_most_popular_time,
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
        ShimmerBox(
            modifier = Modifier
                .width(180.dp)
                .height(24.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        ShimmerBox(
            modifier = Modifier
                .width(60.dp)
                .height(14.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerBox(
            modifier = Modifier
                .width(120.dp)
                .height(32.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        ShimmerBox(
            modifier = Modifier
                .width(80.dp)
                .height(14.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        ShimmerBox(
            modifier = Modifier
                .width(60.dp)
                .height(14.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerBox(
            modifier = Modifier
                .width(100.dp)
                .height(32.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        ShimmerBox(
            modifier = Modifier
                .width(80.dp)
                .height(14.dp)
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
        StatsCardHeader(
            titleResId = R.string
                .stats_insights_most_popular_time,
            onRemoveCard = onRemoveCard,
            cardPosition = cardPosition,
            onMoveUp = onMoveUp,
            onMoveToTop = onMoveToTop,
            onMoveDown = onMoveDown,
            onMoveToBottom = onMoveToBottom
        )
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
    state: MostPopularTimeCardUiState.Loaded,
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
                .stats_insights_most_popular_time,
            onRemoveCard = onRemoveCard,
            cardPosition = cardPosition,
            onMoveUp = onMoveUp,
            onMoveToTop = onMoveToTop,
            onMoveDown = onMoveDown,
            onMoveToBottom = onMoveToBottom
        )
        Spacer(modifier = Modifier.height(12.dp))
        StatSection(
            label = stringResource(
                R.string.stats_insights_best_day
            ),
            value = state.bestDay,
            percent = stringResource(
                R.string
                    .stats_insights_views_percent,
                state.bestDayPercent
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        StatSection(
            label = stringResource(
                R.string.stats_insights_best_hour
            ),
            value = state.bestHour,
            percent = stringResource(
                R.string
                    .stats_insights_views_percent,
                state.bestHourPercent
            )
        )
    }
}

@Composable
private fun StatSection(
    label: String,
    value: String,
    percent: String
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme
            .onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
        text = percent,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme
            .onSurfaceVariant
    )
}

@Preview(showBackground = true)
@Composable
private fun MostPopularTimeCardLoadingPreview() {
    AppThemeM3 {
        MostPopularTimeCard(
            uiState =
                MostPopularTimeCardUiState.Loading,
            onRemoveCard = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MostPopularTimeCardNoDataPreview() {
    AppThemeM3 {
        MostPopularTimeCard(
            uiState =
                MostPopularTimeCardUiState.NoData,
            onRemoveCard = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MostPopularTimeCardLoadedPreview() {
    AppThemeM3 {
        MostPopularTimeCard(
            uiState =
                MostPopularTimeCardUiState.Loaded(
                    bestDay = "Monday",
                    bestDayPercent = "23",
                    bestHour = "4:00 PM",
                    bestHourPercent = "11"
                ),
            onRemoveCard = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MostPopularTimeCardErrorPreview() {
    AppThemeM3 {
        MostPopularTimeCard(
            uiState = MostPopularTimeCardUiState.Error(
                message = "Failed to load stats"
            ),
            onRemoveCard = {},
            onRetry = {}
        )
    }
}
