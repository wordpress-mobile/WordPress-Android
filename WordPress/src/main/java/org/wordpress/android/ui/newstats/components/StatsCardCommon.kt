package org.wordpress.android.ui.newstats.components

import androidx.annotation.StringRes
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.util.formatStatValue

private val CardCornerRadius = 10.dp
private val CardPadding = 16.dp
private val CardMargin = 16.dp

/**
 * Common card container with border, background, and rounded corners.
 * Used by stats cards (Countries, Authors, etc.) for consistent styling.
 */
@Composable
fun StatsCardContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CardMargin, vertical = 8.dp)
            .clip(RoundedCornerShape(CardCornerRadius))
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(CardCornerRadius))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        content()
    }
}

/**
 * Common card header with title and menu.
 * Used by stats cards for consistent header styling.
 */
@Composable
fun StatsCardHeader(
    @StringRes titleResId: Int,
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
            text = stringResource(titleResId),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
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

/**
 * Common empty content state.
 * Displays "No data yet" message when there's no data to show.
 */
@Composable
fun StatsCardEmptyContent() {
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

/**
 * Common error content state with retry button.
 * Displays error message and retry button.
 */
@Composable
fun StatsCardErrorContent(
    @StringRes titleResId: Int,
    errorMessage: String,
    onRetry: () -> Unit,
    onRemoveCard: () -> Unit,
    cardPosition: CardPosition?,
    onMoveUp: (() -> Unit)?,
    onMoveToTop: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onMoveToBottom: (() -> Unit)?
) {
    Column(modifier = Modifier.padding(CardPadding)) {
        StatsCardHeader(
            titleResId = titleResId,
            onRemoveCard = onRemoveCard,
            cardPosition = cardPosition,
            onMoveUp = onMoveUp,
            onMoveToTop = onMoveToTop,
            onMoveDown = onMoveDown,
            onMoveToBottom = onMoveToBottom
        )
        Spacer(modifier = Modifier.height(24.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.retry))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Common "Show All" footer for stats cards.
 * Displays a clickable row with "Show All" text and chevron icon.
 */
@Composable
fun ShowAllFooter(onClick: () -> Unit) {
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

/**
 * Common list header row showing two column headers.
 */
@Composable
fun StatsListHeader(
    @StringRes leftHeaderResId: Int,
    @StringRes rightHeaderResId: Int = R.string.stats_countries_views_header
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(leftHeaderResId),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(rightHeaderResId),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Common row container with background percentage bar.
 * Used for country rows, author rows, and similar list items.
 *
 * @param percentage Fill percentage for the background bar (0f to 1f)
 * @param content The row content to display
 */
@Composable
fun StatsListRowContainer(
    percentage: Float,
    content: @Composable () -> Unit
) {
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)

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

        content()
    }
}

/**
 * Common views column with count and change indicator.
 * Used in list rows to display views count and change percentage.
 */
@Composable
fun StatsViewsColumn(
    views: Long,
    change: StatsViewChange
) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = formatStatValue(views),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        StatsChangeIndicator(change = change)
    }
}

/**
 * Common text element for item name with ellipsis overflow.
 */
@Composable
fun StatsItemName(
    name: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = name,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * Common position number for detail screens.
 */
@Composable
fun StatsPositionNumber(position: Int) {
    Text(
        text = position.toString(),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.width(32.dp)
    )
}

/**
 * Common list item row for stats cards.
 * Displays an icon, name, views count and change indicator with a percentage bar background.
 *
 * @param percentage Fill percentage for the background bar (0f to 1f)
 * @param name The item name to display
 * @param views The views count
 * @param change The change compared to previous period
 * @param icon Composable slot for the icon (flag, avatar, etc.)
 */
@Composable
fun StatsListItem(
    percentage: Float,
    name: String,
    views: Long,
    change: StatsViewChange,
    icon: @Composable () -> Unit
) {
    StatsListRowContainer(percentage = percentage) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(12.dp))
            StatsItemName(name = name, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(12.dp))
            StatsViewsColumn(views = views, change = change)
        }
    }
}

/**
 * Common list item row for detail screens with position number.
 * Displays position, icon, name, views count and change indicator.
 *
 * @param position The position number (1, 2, 3, ...)
 * @param percentage Fill percentage for the background bar (0f to 1f)
 * @param name The item name to display
 * @param views The views count
 * @param change The change compared to previous period
 * @param icon Composable slot for the icon (flag, avatar, etc.)
 */
@Composable
fun StatsDetailListItem(
    position: Int,
    percentage: Float,
    name: String,
    views: Long,
    change: StatsViewChange,
    icon: @Composable () -> Unit
) {
    StatsListRowContainer(percentage = percentage) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatsPositionNumber(position = position)
            icon()
            Spacer(modifier = Modifier.width(12.dp))
            StatsItemName(name = name, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(12.dp))
            StatsViewsColumn(views = views, change = change)
        }
    }
}
