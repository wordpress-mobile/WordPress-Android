package org.wordpress.android.ui.newstats.mostviewed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.newstats.StatsCardType
import org.wordpress.android.ui.newstats.StatsColors
import org.wordpress.android.ui.newstats.components.CardPosition
import org.wordpress.android.ui.newstats.components.ShowAllFooter
import org.wordpress.android.ui.newstats.components.StatsCardMenu
import org.wordpress.android.ui.newstats.util.ShimmerBox
import java.util.Locale

private val CardCornerRadius = 10.dp
private val CardPadding = 16.dp
private val CardMargin = 16.dp
private const val LOADING_SHIMMER_ITEM_COUNT = 5

@Composable
fun MostViewedCard(
    uiState: MostViewedCardUiState,
    cardType: StatsCardType,
    onShowAllClick: () -> Unit,
    onRetry: () -> Unit,
    onRemoveCard: () -> Unit,
    modifier: Modifier = Modifier,
    cardPosition: CardPosition? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveToTop: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onMoveToBottom: (() -> Unit)? = null,
    onOpenWpAdmin: (() -> Unit)? = null,
    onUrlClick: (String) -> Unit = {},
    onItemClick: ((MostViewedItem) -> Unit)? = null
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CardMargin, vertical = 8.dp)
            .clip(RoundedCornerShape(CardCornerRadius))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(CardCornerRadius)
            )
            .background(MaterialTheme.colorScheme.surface)
    ) {
        when (uiState) {
            is MostViewedCardUiState.Loading -> LoadingContent()
            is MostViewedCardUiState.Loaded -> LoadedContent(
                uiState, cardType, onShowAllClick, onRemoveCard,
                cardPosition, onMoveUp, onMoveToTop, onMoveDown, onMoveToBottom,
                onUrlClick, onItemClick
            )
            is MostViewedCardUiState.Error -> ErrorContent(
                uiState, cardType, onRetry, onRemoveCard,
                cardPosition, onMoveUp, onMoveToTop, onMoveDown, onMoveToBottom,
                onOpenWpAdmin
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
        // Header shimmer
        ShimmerBox(
            modifier = Modifier
                .width(100.dp)
                .height(24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Column headers shimmer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
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
        // Items shimmer
        repeat(LOADING_SHIMMER_ITEM_COUNT) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.End) {
                    ShimmerBox(
                        modifier = Modifier
                            .width(40.dp)
                            .height(16.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .width(50.dp)
                            .height(12.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Show All shimmer
        ShimmerBox(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(80.dp)
                .height(16.dp)
        )
    }
}

@Composable
private fun LoadedContent(
    state: MostViewedCardUiState.Loaded,
    cardType: StatsCardType,
    onShowAllClick: () -> Unit,
    onRemoveCard: () -> Unit,
    cardPosition: CardPosition?,
    onMoveUp: (() -> Unit)?,
    onMoveToTop: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onMoveToBottom: (() -> Unit)?,
    onUrlClick: (String) -> Unit,
    onItemClick: ((MostViewedItem) -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CardPadding)
    ) {
        HeaderSection(
            cardType = cardType,
            onRemoveCard = onRemoveCard,
            cardPosition = cardPosition,
            onMoveUp = onMoveUp,
            onMoveToTop = onMoveToTop,
            onMoveDown = onMoveDown,
            onMoveToBottom = onMoveToBottom
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (state.items.isEmpty()) {
            EmptyStateContent()
        } else {
            ColumnHeadersRow(cardType = cardType)
            Spacer(modifier = Modifier.height(8.dp))
            state.items.forEachIndexed { index, item ->
                val percentage = if (state.maxViewsForBar > 0) {
                    item.views.toFloat() / state.maxViewsForBar.toFloat()
                } else 0f
                // Key on the index plus the title: the index guarantees uniqueness (titles can
                // repeat), while the title resets the row's saved expanded state when a different
                // entry lands at this position after a reload. rememberSaveable keeps it across
                // rotation.
                //
                // Deliberately not item.id — that only tracks content for some card types. Posts
                // use the backend id and referrers a name hash, but clicks, search terms, video
                // plays and file downloads number their items by position, so the id would never
                // change and a refresh could leave a row expanded onto whatever replaced it.
                key(index, item.title) {
                    MostViewedExpandableRow(
                        title = item.title,
                        views = item.views,
                        change = item.change,
                        children = item.children,
                        percentage = percentage,
                        onUrlClick = onUrlClick,
                        url = item.url,
                        onItemClick = onItemClick?.let { { it(item) } }
                    )
                }
                if (index < state.items.lastIndex) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            ShowAllFooter(onClick = onShowAllClick)
        }
    }
}

@Composable
private fun HeaderSection(
    cardType: StatsCardType,
    onRemoveCard: () -> Unit,
    cardPosition: CardPosition?,
    onMoveUp: (() -> Unit)?,
    onMoveToTop: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onMoveToBottom: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(cardType.displayNameResId),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
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
}

@Composable
private fun ColumnHeadersRow(cardType: StatsCardType) {
    val headerResId = when (cardType) {
        StatsCardType.MOST_VIEWED_POSTS_AND_PAGES ->
            R.string.stats_most_viewed_title_header
        StatsCardType.MOST_VIEWED_REFERRERS ->
            R.string.stats_most_viewed_referrer_header
        StatsCardType.CLICKS -> R.string.stats_clicks_link_label
        StatsCardType.SEARCH_TERMS -> R.string.stats_search_terms_label
        StatsCardType.VIDEO_PLAYS -> R.string.stats_videos_title_label
        StatsCardType.FILE_DOWNLOADS ->
            R.string.stats_file_downloads_title_label
        else -> cardType.displayNameResId
    }

    val valueHeaderResId = when (cardType) {
        StatsCardType.CLICKS -> R.string.stats_clicks_label
        StatsCardType.FILE_DOWNLOADS ->
            R.string.stats_file_downloads_value_label
        else -> R.string.stats_views
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(headerResId),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = stringResource(valueHeaderResId),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun ChangeIndicator(change: MostViewedChange) {
    val (text, color) = when (change) {
        is MostViewedChange.Positive -> Pair(
            String.format(Locale.getDefault(), "+%.1f%%", change.percentage),
            StatsColors.ChangeBadgePositive
        )
        is MostViewedChange.Negative -> Pair(
            String.format(Locale.getDefault(), "-%.1f%%", change.percentage),
            StatsColors.ChangeBadgeNegative
        )
        is MostViewedChange.NoChange -> return
        is MostViewedChange.NotAvailable -> return
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color
    )
}

@Composable
private fun EmptyStateContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.stats_no_data_yet),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(
    state: MostViewedCardUiState.Error,
    cardType: StatsCardType,
    onRetry: () -> Unit,
    onRemoveCard: () -> Unit,
    cardPosition: CardPosition?,
    onMoveUp: (() -> Unit)?,
    onMoveToTop: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onMoveToBottom: (() -> Unit)?,
    onOpenWpAdmin: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CardPadding)
    ) {
        HeaderSection(
            cardType = cardType,
            onRemoveCard = onRemoveCard,
            cardPosition = cardPosition,
            onMoveUp = onMoveUp,
            onMoveToTop = onMoveToTop,
            onMoveDown = onMoveDown,
            onMoveToBottom = onMoveToBottom
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            if (onOpenWpAdmin != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onOpenWpAdmin) {
                    Text(
                        text = stringResource(
                            R.string.my_site_btn_wp_admin
                        )
                    )
                }
            } else if (!state.isNotAvailable) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry) {
                    Text(text = stringResource(R.string.retry))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MostViewedCardLoadingPreview() {
    AppThemeM3 {
        MostViewedCard(
            uiState = MostViewedCardUiState.Loading,
            cardType = StatsCardType.MOST_VIEWED_POSTS_AND_PAGES,
            onShowAllClick = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MostViewedCardLoadedPreview() {
    AppThemeM3 {
        MostViewedCard(
            uiState = MostViewedCardUiState.Loaded(
                items = listOf(
                    MostViewedItem(
                        id = 1,
                        title = "Welcome to Automattic",
                        views = 417,
                        change = MostViewedChange.Negative(194, 31.8)
                    ),
                    MostViewedItem(
                        id = 2,
                        title = "Travel Guidelines",
                        views = 58,
                        change = MostViewedChange.NoChange
                    ),
                    MostViewedItem(
                        id = 3,
                        title = "Expense Guidelines",
                        views = 42,
                        change = MostViewedChange.Positive(10, 31.2)
                    ),
                    MostViewedItem(
                        id = 4,
                        title = "Getting Started with Claude Code: A Comprehensive Tutorial",
                        views = 38,
                        change = MostViewedChange.Positive(4, 11.8)
                    ),
                    MostViewedItem(
                        id = 5,
                        title = "GitHub Enterprise for Automattic",
                        views = 35,
                        change = MostViewedChange.Positive(23, 191.7)
                    )
                ),
                maxViewsForBar = 417
            ),
            cardType = StatsCardType.MOST_VIEWED_POSTS_AND_PAGES,
            onShowAllClick = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MostViewedCardErrorPreview() {
    AppThemeM3 {
        MostViewedCard(
            uiState = MostViewedCardUiState.Error(
                message = "Failed to load data"
            ),
            cardType = StatsCardType.MOST_VIEWED_POSTS_AND_PAGES,
            onShowAllClick = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MostViewedCardLoadedDarkPreview() {
    AppThemeM3 {
        MostViewedCard(
            uiState = MostViewedCardUiState.Loaded(
                items = listOf(
                    MostViewedItem(
                        id = 1,
                        title = "Welcome to Automattic",
                        views = 417,
                        change = MostViewedChange.Negative(194, 31.8)
                    ),
                    MostViewedItem(
                        id = 2,
                        title = "Travel Guidelines",
                        views = 58,
                        change = MostViewedChange.NoChange
                    )
                ),
                maxViewsForBar = 417
            ),
            cardType = StatsCardType.MOST_VIEWED_POSTS_AND_PAGES,
            onShowAllClick = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}
