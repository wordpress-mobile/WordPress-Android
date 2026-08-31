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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.components.StatsListRowContainer
import org.wordpress.android.ui.newstats.components.StatsOpenLinkIcon
import org.wordpress.android.ui.newstats.util.formatStatValue

/**
 * Expandable Most Viewed row (group title, views, change badge, and an optional expand/collapse
 * chevron that reveals indented child rows). Shared by the card ([MostViewedCard]) and the detail
 * screen ([MostViewedDetailActivity]); they differ only by the leading [position] number (detail
 * only) and the child [childStartPadding] indent.
 *
 * What a tap does — expand, open a link, or hand off to the caller — is decided by
 * [statsRowAction]; see there for the precedence and why it matters.
 *
 * The caller is expected to wrap this row in a `key(...)` so the [rememberSaveable] expanded state
 * is scoped to the item and survives configuration changes (e.g. rotation).
 *
 * @param title The group title
 * @param views The group's view count
 * @param change The change badge shown under the views
 * @param children The child rows revealed when expanded (empty = no chevron, not expandable)
 * @param percentage Bar fill percentage (0f..1f)
 * @param onUrlClick Invoked with the URL when a linkable row (this row or a child) is tapped
 * @param url The URL passed to [onUrlClick] when this row is tapped
 * @param onItemClick Invoked when a childless row is tapped, e.g. opens a post's detail stats
 * @param position Optional 1-based position number shown on the detail screen
 * @param childStartPadding Indent applied to the expanded child rows
 */
@Composable
internal fun MostViewedExpandableRow(
    title: String,
    views: Long,
    change: MostViewedChange,
    children: List<MostViewedChildItem>,
    percentage: Float,
    onUrlClick: (String) -> Unit,
    url: String? = null,
    onItemClick: (() -> Unit)? = null,
    position: Int? = null,
    childStartPadding: Dp = 24.dp
) {
    val hasChildren = children.isNotEmpty()
    val action = statsRowAction(hasChildren, url, onItemClick)
    val linkUrl = (action as? StatsRowAction.OpenUrl)?.url
    var expanded by rememberSaveable { mutableStateOf(false) }
    val clickModifier = when (action) {
        StatsRowAction.Expand -> Modifier.clickable { expanded = !expanded }
        is StatsRowAction.Item -> Modifier.clickable { action.onClick() }
        is StatsRowAction.OpenUrl -> Modifier.clickable { onUrlClick(action.url) }
        StatsRowAction.None -> Modifier
    }

    Column {
        StatsListRowContainer(percentage = percentage, modifier = clickModifier) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (position != null) {
                    Text(
                        text = position.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(32.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasChildren) {
                            MostViewedExpandChevron(expanded = expanded)
                            Spacer(modifier = Modifier.width(4.dp))
                        } else if (linkUrl != null) {
                            StatsOpenLinkIcon()
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = formatStatValue(views),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    ChangeIndicator(change = change)
                }
            }
        }

        MostViewedExpandableChildren(
            children = children,
            expanded = expanded,
            startPadding = childStartPadding,
            onUrlClick = onUrlClick
        )
    }
}

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
    onUrlClick: (String) -> Unit
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
                MostViewedChildRow(child = child, percentage = childPercentage, onUrlClick = onUrlClick)
            }
        }
    }
}

@Composable
private fun MostViewedChildRow(
    child: MostViewedChildItem,
    percentage: Float,
    onUrlClick: (String) -> Unit
) {
    val url = child.url
    val isClickable = !url.isNullOrBlank()
    val clickModifier = if (isClickable) Modifier.clickable { onUrlClick(url) } else Modifier

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
                StatsOpenLinkIcon()
            }
        }
    }
}
