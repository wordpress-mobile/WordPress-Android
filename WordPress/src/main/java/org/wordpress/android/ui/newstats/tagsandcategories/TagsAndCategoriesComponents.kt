package org.wordpress.android.ui.newstats.tagsandcategories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.components.StatsListRowContainer
import org.wordpress.android.ui.newstats.util.formatStatValue

private const val VERTICAL_LINE_ALPHA = 0.3f

@Composable
fun TagTypeIcon(
    displayType: TagGroupDisplayType
) {
    Icon(
        imageVector = when (displayType) {
            TagGroupDisplayType.CATEGORY ->
                Icons.Outlined.Folder
            TagGroupDisplayType.TAG,
            TagGroupDisplayType.MIXED ->
                Icons.Outlined.Sell
        },
        contentDescription = when (displayType) {
            TagGroupDisplayType.CATEGORY ->
                stringResource(
                    R.string.stats_tag_type_category
                )
            TagGroupDisplayType.TAG,
            TagGroupDisplayType.MIXED ->
                stringResource(
                    R.string.stats_tag_type_tag
                )
        },
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme
            .onSurfaceVariant
    )
}

@Composable
@Suppress("LongParameterList")
fun TagGroupRow(
    item: TagGroupUiItem,
    percentage: Float,
    isExpandable: Boolean,
    isExpanded: Boolean,
    onClick: (() -> Unit)?,
    position: Int? = null
) {
    StatsListRowContainer(
        percentage = percentage,
        modifier = if (onClick != null) {
            Modifier.clickable(onClick = onClick)
        } else {
            Modifier
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 12.dp,
                    horizontal = 16.dp
                ),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                if (position != null) {
                    Text(
                        text = "$position",
                        style = MaterialTheme.typography
                            .bodyLarge,
                        color = MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                        modifier =
                            Modifier.width(32.dp)
                    )
                }
                TagTypeIcon(
                    displayType = item.displayType
                )
                Spacer(
                    modifier = Modifier.width(8.dp)
                )
                if (isExpandable) {
                    Icon(
                        imageVector =
                            if (isExpanded) {
                                Icons.Default
                                    .KeyboardArrowUp
                            } else {
                                Icons.Default
                                    .KeyboardArrowDown
                            },
                        contentDescription =
                            stringResource(
                                if (isExpanded) {
                                    R.string
                                        .stats_collapse_group
                                } else {
                                    R.string
                                        .stats_expand_group
                                }
                            ),
                        modifier =
                            Modifier.size(16.dp),
                        tint = MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                    )
                    Spacer(
                        modifier =
                            Modifier.width(4.dp)
                    )
                }
                Text(
                    text = item.name,
                    style = MaterialTheme.typography
                        .bodyLarge,
                    color = MaterialTheme.colorScheme
                        .onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = formatStatValue(item.views),
                style = MaterialTheme.typography
                    .bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme
                    .onSurface
            )
        }
    }
}

@Composable
fun ExpandedTagsSection(
    tags: List<TagUiItem>,
    startPadding: Dp = 24.dp
) {
    val lineColor = MaterialTheme.colorScheme.primary
        .copy(alpha = VERTICAL_LINE_ALPHA)

    Column(
        modifier = Modifier.padding(
            start = startPadding,
            top = 4.dp,
            bottom = 4.dp
        )
    ) {
        tags.forEachIndexed { index, tag ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(24.dp)
                        .background(lineColor)
                )
                Spacer(
                    modifier = Modifier.width(12.dp)
                )
                TagTypeIcon(
                    displayType =
                        TagGroupDisplayType
                            .fromTagType(tag.tagType)
                )
                Spacer(
                    modifier = Modifier.width(8.dp)
                )
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography
                        .bodyLarge,
                    color = MaterialTheme.colorScheme
                        .onSurface
                )
            }
            if (index < tags.lastIndex) {
                Spacer(
                    modifier = Modifier.height(2.dp)
                )
            }
        }
    }
}
