package org.wordpress.android.ui.newstats.subscribers.alltimestats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.components.CardPosition
import org.wordpress.android.ui.newstats.components.StatsCardContainer
import org.wordpress.android.ui.newstats.components.StatsCardEmptyContent
import org.wordpress.android.ui.newstats.components.StatsCardErrorContent
import org.wordpress.android.ui.newstats.components.StatsCardHeader
import org.wordpress.android.ui.newstats.util.ShimmerBox
import org.wordpress.android.ui.newstats.util.formatStatValue

private val CardPadding = 16.dp

@Suppress("LongParameterList")
@Composable
fun AllTimeSubscribersCard(
    uiState: AllTimeSubscribersUiState,
    onRetry: () -> Unit,
    onRemoveCard: () -> Unit,
    modifier: Modifier = Modifier,
    cardPosition: CardPosition? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveToTop: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onMoveToBottom: (() -> Unit)? = null
) {
    StatsCardContainer(modifier = modifier) {
        when (uiState) {
            is AllTimeSubscribersUiState.Loading ->
                LoadingContent()
            is AllTimeSubscribersUiState.Loaded ->
                LoadedContent(
                    state = uiState,
                    onRemoveCard = onRemoveCard,
                    cardPosition = cardPosition,
                    onMoveUp = onMoveUp,
                    onMoveToTop = onMoveToTop,
                    onMoveDown = onMoveDown,
                    onMoveToBottom = onMoveToBottom
                )
            is AllTimeSubscribersUiState.Error ->
                StatsCardErrorContent(
                    titleResId =
                        R.string.stats_subscribers_all_time,
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
                .fillMaxWidth(0.4f)
                .height(24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            repeat(2) {
                Column {
                    ShimmerBox(
                        modifier = Modifier
                            .height(32.dp)
                            .fillMaxWidth(0.3f)
                    )
                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )
                    ShimmerBox(
                        modifier = Modifier
                            .height(16.dp)
                            .fillMaxWidth(0.25f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            repeat(2) {
                Column {
                    ShimmerBox(
                        modifier = Modifier
                            .height(32.dp)
                            .fillMaxWidth(0.3f)
                    )
                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )
                    ShimmerBox(
                        modifier = Modifier
                            .height(16.dp)
                            .fillMaxWidth(0.25f)
                    )
                }
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun LoadedContent(
    state: AllTimeSubscribersUiState.Loaded,
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
            titleResId =
                R.string.stats_subscribers_all_time,
            onRemoveCard = onRemoveCard,
            cardPosition = cardPosition,
            onMoveUp = onMoveUp,
            onMoveToTop = onMoveToTop,
            onMoveDown = onMoveDown,
            onMoveToBottom = onMoveToBottom
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = stringResource(
                    R.string.stats_subscribers_current
                ),
                value = state.currentCount
            )
            StatItem(
                label = stringResource(
                    R.string.stats_subscribers_30_days_ago
                ),
                value = state.count30DaysAgo
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = stringResource(
                    R.string.stats_subscribers_60_days_ago
                ),
                value = state.count60DaysAgo
            )
            StatItem(
                label = stringResource(
                    R.string.stats_subscribers_90_days_ago
                ),
                value = state.count90DaysAgo
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: Long) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formatStatValue(value),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
