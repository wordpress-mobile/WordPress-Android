package org.wordpress.android.ui.newstats.mostviewed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.components.StatsListRowContainer
import org.wordpress.android.ui.newstats.util.formatStatValue

/**
 * Expand/collapse chevron shown on a Most Viewed row that has children.
 * Shared by the card ([MostViewedCard]) and the detail screen ([MostViewedDetailActivity]).
 */
@Composable
internal fun MostViewedExpandChevron(expanded: Boolean) {
    Icon(
        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
        contentDescription = stringResource(
            if (expanded) R.string.stats_collapse_group else R.string.stats_expand_group
        ),
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Indented, animated list of child rows shown when a Most Viewed group is expanded.
 * Shared by the card and the detail screen; only the [startPadding] indent differs.
 */
@Composable
internal fun MostViewedExpandableChildren(
    children: List<MostViewedChildItem>,
    expanded: Boolean,
    startPadding: Dp,
    onChildClick: (String) -> Unit
) {
    AnimatedVisibility(visible = expanded) {
        val maxChildViews = children.maxOfOrNull { it.views } ?: 0L
        Column(
            modifier = Modifier.padding(start = startPadding, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            children.forEach { child ->
                val childPercentage = if (maxChildViews > 0) {
                    child.views.toFloat() / maxChildViews.toFloat()
                } else 0f
                MostViewedChildRow(child = child, percentage = childPercentage, onChildClick = onChildClick)
            }
        }
    }
}

@Composable
private fun MostViewedChildRow(
    child: MostViewedChildItem,
    percentage: Float,
    onChildClick: (String) -> Unit
) {
    val url = child.url
    val isClickable = !url.isNullOrBlank()
    val clickModifier = if (isClickable) Modifier.clickable { onChildClick(url) } else Modifier

    StatsListRowContainer(percentage = percentage, modifier = clickModifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = child.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatStatValue(child.views),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isClickable) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
