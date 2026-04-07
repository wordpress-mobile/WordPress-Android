package org.wordpress.android.ui.newstats.utm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.newstats.components.StatsItemName
import org.wordpress.android.ui.newstats.components.StatsListRowContainer
import org.wordpress.android.ui.newstats.util.formatStatValue

/**
 * Expandable UTM row used in both the card and detail screen.
 *
 * @param item The UTM item to display
 * @param percentage Bar fill percentage (0f..1f)
 * @param maxViewsForBar The max views value used to
 *     compute bar widths for child post rows
 * @param position Optional 1-based position number shown
 *     in the detail screen
 */
@Composable
fun UtmExpandableRow(
    item: UtmUiItem,
    percentage: Float,
    maxViewsForBar: Long,
    position: Int? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val hasTopPosts = item.topPosts.isNotEmpty()

    Column {
        StatsListRowContainer(percentage = percentage) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (hasTopPosts) {
                            Modifier.clickable {
                                expanded = !expanded
                            }
                        } else {
                            Modifier
                        }
                    )
                    .padding(
                        horizontal = 8.dp,
                        vertical = 10.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                if (position != null) {
                    Text(
                        text = "$position",
                        style = MaterialTheme.typography
                            .bodySmall,
                        color = MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                        modifier = Modifier
                            .width(24.dp)
                    )
                }
                StatsItemName(
                    name = item.title,
                    modifier = Modifier.weight(1f)
                )
                if (hasTopPosts) {
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Default
                                .KeyboardArrowUp
                        } else {
                            Icons.Default
                                .KeyboardArrowDown
                        },
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp),
                        tint = MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                    )
                }
                Spacer(
                    modifier = Modifier.width(4.dp)
                )
                Text(
                    text = formatStatValue(
                        item.views
                    ),
                    style = MaterialTheme.typography
                        .bodyMedium,
                    color = MaterialTheme.colorScheme
                        .onSurface
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(
                    start = 24.dp
                )
            ) {
                item.topPosts.forEach { post ->
                    val postPct =
                        if (maxViewsForBar > 0) {
                            post.views.toFloat() /
                                maxViewsForBar
                                    .toFloat()
                        } else {
                            0f
                        }
                    UtmPostRow(
                        post = post,
                        percentage = postPct
                    )
                }
            }
        }
    }
}

@Composable
fun UtmPostRow(
    post: UtmPostUiItem,
    percentage: Float = 0f
) {
    StatsListRowContainer(percentage = percentage) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 8.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text = post.title,
                style = MaterialTheme.typography
                    .bodySmall,
                color = MaterialTheme.colorScheme
                    .onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(
                modifier = Modifier.width(8.dp)
            )
            Text(
                text = formatStatValue(post.views),
                style = MaterialTheme.typography
                    .bodySmall,
                color = MaterialTheme.colorScheme
                    .onSurface
            )
        }
    }
}
