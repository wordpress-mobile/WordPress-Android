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


@Suppress("TooGenericExceptionCaught", "SwallowedException")
internal fun formatSubscriberDate(
    dateString: String,
    resources: android.content.res.Resources
): String {
    return try {
        val subscribed = parseSubscriberDate(dateString)
        val today = java.time.LocalDate.now()
        val period = java.time.Period.between(
            subscribed, today
        )
        val totalDays =
            java.time.temporal.ChronoUnit.DAYS
                .between(subscribed, today)
        when {
            totalDays < 1L -> resources.getString(
                R.string.stats_subscriber_since_today
            )
            period.years < 1 -> resources
                .getQuantityString(
                    R.plurals.stats_subscriber_days,
                    totalDays.toInt(), totalDays.toInt()
                )
            else -> formatYearsAndDays(
                subscribed, today, period, resources
            )
        }
    } catch (_: Exception) {
        dateString
    }
}

private fun parseSubscriberDate(
    dateString: String
): java.time.LocalDate = try {
    java.time.LocalDateTime.parse(
        dateString,
        java.time.format.DateTimeFormatter.ISO_DATE_TIME
    ).toLocalDate()
} catch (_: Exception) {
    java.time.LocalDate.parse(
        dateString,
        java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
    )
}

private fun formatYearsAndDays(
    subscribed: java.time.LocalDate,
    today: java.time.LocalDate,
    period: java.time.Period,
    resources: android.content.res.Resources
): String {
    val years = period.years
    val remaining =
        java.time.temporal.ChronoUnit.DAYS.between(
            subscribed.plusYears(years.toLong()), today
        )
    val yearsPart = resources.getQuantityString(
        R.plurals.stats_subscriber_years, years, years
    )
    if (remaining == 0L) return yearsPart
    val daysPart = resources.getQuantityString(
        R.plurals.stats_subscriber_days,
        remaining.toInt(), remaining.toInt()
    )
    return resources.getString(
        R.string.stats_subscriber_years_and_days,
        yearsPart, daysPart
    )
}
