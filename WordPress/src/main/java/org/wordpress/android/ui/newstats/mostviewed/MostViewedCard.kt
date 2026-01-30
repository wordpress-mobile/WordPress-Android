package org.wordpress.android.ui.newstats.mostviewed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.newstats.StatsColors
import org.wordpress.android.ui.newstats.util.ShimmerBox
import org.wordpress.android.ui.newstats.util.formatStatValue
import java.util.Locale

private val CardCornerRadius = 10.dp
private val CardPadding = 16.dp
private val CardMargin = 16.dp
private const val HIGHLIGHTED_ITEM_BACKGROUND_ALPHA = 0.08f
private const val LOADING_SHIMMER_ITEM_COUNT = 5

@Composable
fun MostViewedCard(
    uiState: MostViewedCardUiState,
    onDataSourceChanged: (MostViewedDataSource) -> Unit,
    onShowAllClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
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
            is MostViewedCardUiState.Loaded -> LoadedContent(uiState, onDataSourceChanged, onShowAllClick)
            is MostViewedCardUiState.Error -> ErrorContent(uiState, onRetry)
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
    onDataSourceChanged: (MostViewedDataSource) -> Unit,
    onShowAllClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CardPadding)
    ) {
        HeaderSection()
        Spacer(modifier = Modifier.height(12.dp))
        ColumnHeadersRow(
            selectedDataSource = state.selectedDataSource,
            onDataSourceChanged = onDataSourceChanged
        )
        Spacer(modifier = Modifier.height(8.dp))
        state.items.forEachIndexed { index, item ->
            val percentage = if (state.maxViewsForBar > 0) {
                item.views.toFloat() / state.maxViewsForBar.toFloat()
            } else 0f
            MostViewedItemRow(item = item, percentage = percentage)
            if (index < state.items.lastIndex) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        if (state.items.isEmpty()) {
            EmptyStateContent()
        }
        Spacer(modifier = Modifier.height(12.dp))
        ShowAllFooter(onClick = onShowAllClick)
    }
}

@Composable
private fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.stats_most_viewed_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = { /* Future menu actions */ }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ColumnHeadersRow(
    selectedDataSource: MostViewedDataSource,
    onDataSourceChanged: (MostViewedDataSource) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Row(
                modifier = Modifier
                    .clickable { expanded = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(selectedDataSource.labelResId),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                MostViewedDataSource.entries.forEach { dataSource ->
                    DropdownMenuItem(
                        text = { Text(stringResource(dataSource.labelResId)) },
                        onClick = {
                            onDataSourceChanged(dataSource)
                            expanded = false
                        },
                        trailingIcon = if (dataSource == selectedDataSource) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else {
                            null
                        }
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.stats_views),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MostViewedItemRow(item: MostViewedItem, percentage: Float) {
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = HIGHLIGHTED_ITEM_BACKGROUND_ALPHA)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Background bar representing the percentage
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = percentage)
                .fillMaxHeight()
                .background(barColor)
        )

        // Content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatStatValue(item.views),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ChangeIndicator(change = item.change)
                }
            }
        }
    }
}

@Composable
private fun ChangeIndicator(change: MostViewedChange) {
    val (text, color) = when (change) {
        is MostViewedChange.Positive -> Pair(
            "+${formatStatValue(change.value)} (${
                String.format(Locale.getDefault(), "%.1f%%", change.percentage)
            })",
            StatsColors.ChangeBadgePositive
        )
        is MostViewedChange.Negative -> Pair(
            "-${formatStatValue(change.value)} (${
                String.format(Locale.getDefault(), "%.1f%%", change.percentage)
            })",
            StatsColors.ChangeBadgeNegative
        )
        is MostViewedChange.NoChange -> Pair(
            "+0 (0%)",
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        is MostViewedChange.NotAvailable -> return
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
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
private fun ShowAllFooter(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.stats_show_all),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ErrorContent(
    state: MostViewedCardUiState.Error,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CardPadding)
    ) {
        Text(
            text = stringResource(R.string.stats_most_viewed_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.retry))
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
            onDataSourceChanged = {},
            onShowAllClick = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MostViewedCardLoadedPreview() {
    AppThemeM3 {
        MostViewedCard(
            uiState = MostViewedCardUiState.Loaded(
                selectedDataSource = MostViewedDataSource.POSTS_AND_PAGES,
                items = listOf(
                    MostViewedItem(
                        id = 1,
                        title = "Welcome to Automattic",
                        views = 417,
                        change = MostViewedChange.Negative(194, 31.8),
                        isHighlighted = true
                    ),
                    MostViewedItem(
                        id = 2,
                        title = "Travel Guidelines",
                        views = 58,
                        change = MostViewedChange.NoChange,
                        isHighlighted = false
                    ),
                    MostViewedItem(
                        id = 3,
                        title = "Expense Guidelines",
                        views = 42,
                        change = MostViewedChange.Positive(10, 31.2),
                        isHighlighted = false
                    ),
                    MostViewedItem(
                        id = 4,
                        title = "Getting Started with Claude Code: A Comprehensive Tutorial",
                        views = 38,
                        change = MostViewedChange.Positive(4, 11.8),
                        isHighlighted = false
                    ),
                    MostViewedItem(
                        id = 5,
                        title = "GitHub Enterprise for Automattic",
                        views = 35,
                        change = MostViewedChange.Positive(23, 191.7),
                        isHighlighted = false
                    )
                ),
                maxViewsForBar = 417
            ),
            onDataSourceChanged = {},
            onShowAllClick = {},
            onRetry = {}
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
            onDataSourceChanged = {},
            onShowAllClick = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MostViewedCardLoadedDarkPreview() {
    AppThemeM3 {
        MostViewedCard(
            uiState = MostViewedCardUiState.Loaded(
                selectedDataSource = MostViewedDataSource.POSTS_AND_PAGES,
                items = listOf(
                    MostViewedItem(
                        id = 1,
                        title = "Welcome to Automattic",
                        views = 417,
                        change = MostViewedChange.Negative(194, 31.8),
                        isHighlighted = true
                    ),
                    MostViewedItem(
                        id = 2,
                        title = "Travel Guidelines",
                        views = 58,
                        change = MostViewedChange.NoChange,
                        isHighlighted = false
                    )
                ),
                maxViewsForBar = 417
            ),
            onDataSourceChanged = {},
            onShowAllClick = {},
            onRetry = {}
        )
    }
}
