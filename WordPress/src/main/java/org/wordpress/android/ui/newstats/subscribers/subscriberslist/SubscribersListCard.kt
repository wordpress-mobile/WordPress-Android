package org.wordpress.android.ui.newstats.subscribers.subscriberslist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.components.CardPosition
import org.wordpress.android.ui.newstats.components.ShowAllFooter
import org.wordpress.android.ui.newstats.components.StatsCardContainer
import org.wordpress.android.ui.newstats.components.StatsCardEmptyContent
import org.wordpress.android.ui.newstats.components.StatsCardErrorContent
import org.wordpress.android.ui.newstats.components.StatsCardHeader
import org.wordpress.android.ui.newstats.util.ShimmerBox

private val CardPadding = 16.dp
private const val LOADING_SHIMMER_ITEM_COUNT = 5

@Suppress("LongParameterList")
@Composable
fun SubscribersListCard(
    uiState: SubscribersListUiState,
    onShowAllClick: () -> Unit,
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
            is SubscribersListUiState.Loading ->
                LoadingContent()
            is SubscribersListUiState.Loaded ->
                LoadedContent(
                    state = uiState,
                    onShowAllClick = onShowAllClick,
                    onRemoveCard = onRemoveCard,
                    cardPosition = cardPosition,
                    onMoveUp = onMoveUp,
                    onMoveToTop = onMoveToTop,
                    onMoveDown = onMoveDown,
                    onMoveToBottom = onMoveToBottom
                )
            is SubscribersListUiState.Error ->
                StatsCardErrorContent(
                    titleResId =
                        R.string.stats_subscribers_list,
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
                .width(100.dp)
                .height(24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            ShimmerBox(
                modifier = Modifier
                    .width(120.dp)
                    .height(20.dp)
            )
            ShimmerBox(
                modifier = Modifier
                    .width(50.dp)
                    .height(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        repeat(LOADING_SHIMMER_ITEM_COUNT) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                )
                Spacer(
                    modifier = Modifier.width(16.dp)
                )
                ShimmerBox(
                    modifier = Modifier
                        .width(80.dp)
                        .height(16.dp)
                )
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun LoadedContent(
    state: SubscribersListUiState.Loaded,
    onShowAllClick: () -> Unit,
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
                R.string.stats_subscribers_list,
            onRemoveCard = onRemoveCard,
            cardPosition = cardPosition,
            onMoveUp = onMoveUp,
            onMoveToTop = onMoveToTop,
            onMoveDown = onMoveDown,
            onMoveToBottom = onMoveToBottom
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (state.items.isEmpty()) {
            StatsCardEmptyContent()
        } else {
            // Column headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(
                        R.string
                            .stats_subscribers_subscriber_header
                    ),
                    style = MaterialTheme
                        .typography.labelMedium,
                    color = MaterialTheme
                        .colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        R.string
                            .stats_subscribers_since_header
                    ),
                    style = MaterialTheme
                        .typography.labelMedium,
                    color = MaterialTheme
                        .colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            state.items.forEachIndexed { index, item ->
                SubscriberItemRow(item = item)
                if (index < state.items.lastIndex) {
                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            ShowAllFooter(onClick = onShowAllClick)
        }
    }
}

@Composable
private fun SubscriberItemRow(
    item: SubscriberListItem
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme
                .colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = formatSubscriberDate(
                item.subscribedSince
            ),
            style = MaterialTheme
                .typography.bodySmall,
            color = MaterialTheme
                .colorScheme.onSurfaceVariant
        )
    }
}

@Suppress("TooGenericExceptionCaught", "SwallowedException")
internal fun formatSubscriberDate(
    dateString: String
): String {
    return try {
        val inputFormat =
            java.time.format.DateTimeFormatter
                .ISO_DATE_TIME
        val outputFormat =
            java.time.format.DateTimeFormatter
                .ofPattern(
                    "MMM d, yyyy",
                    java.util.Locale.getDefault()
                )
        val dateTime =
            java.time.LocalDateTime.parse(
                dateString, inputFormat
            )
        dateTime.format(outputFormat)
    } catch (e: Exception) {
        try {
            val inputFormat =
                java.time.format.DateTimeFormatter
                    .ISO_LOCAL_DATE
            val outputFormat =
                java.time.format.DateTimeFormatter
                    .ofPattern(
                        "MMM d, yyyy",
                        java.util.Locale.getDefault()
                    )
            val date = java.time.LocalDate.parse(
                dateString, inputFormat
            )
            date.format(outputFormat)
        } catch (e2: Exception) {
            dateString
        }
    }
}
