package org.wordpress.android.ui.newstats.tagsandcategories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
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
import org.wordpress.android.ui.newstats.components.CardPosition
import org.wordpress.android.ui.newstats.components.ShowAllFooter
import org.wordpress.android.ui.newstats.components.StatsCardMenu
import org.wordpress.android.ui.newstats.util.ShimmerBox
import org.wordpress.android.ui.newstats.util.formatStatValue

private val CardCornerRadius = 10.dp
private val CardPadding = 16.dp
private val CardMargin = 16.dp
private const val BAR_BACKGROUND_ALPHA = 0.08f
private const val LOADING_SHIMMER_ITEM_COUNT = 5
private const val VERTICAL_LINE_ALPHA = 0.3f

@Composable
@Suppress("LongParameterList")
fun TagsAndCategoriesCard(
    uiState: TagsAndCategoriesCardUiState,
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
    val borderColor =
        MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = CardMargin,
                vertical = 8.dp
            )
            .clip(RoundedCornerShape(CardCornerRadius))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(
                    CardCornerRadius
                )
            )
            .background(MaterialTheme.colorScheme.surface)
    ) {
        when (uiState) {
            is TagsAndCategoriesCardUiState.Loading ->
                LoadingContent()
            is TagsAndCategoriesCardUiState.Loaded ->
                LoadedContent(
                    uiState,
                    onShowAllClick,
                    onRemoveCard,
                    cardPosition,
                    onMoveUp,
                    onMoveToTop,
                    onMoveDown,
                    onMoveToBottom
                )
            is TagsAndCategoriesCardUiState.Error ->
                ErrorContent(
                    uiState,
                    onRetry,
                    onRemoveCard,
                    cardPosition,
                    onMoveUp,
                    onMoveToTop,
                    onMoveDown,
                    onMoveToBottom
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
                .width(140.dp)
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
                        .width(40.dp)
                        .height(16.dp)
                )
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun LoadedContent(
    state: TagsAndCategoriesCardUiState.Loaded,
    onShowAllClick: () -> Unit,
    onRemoveCard: () -> Unit,
    cardPosition: CardPosition?,
    onMoveUp: (() -> Unit)?,
    onMoveToTop: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onMoveToBottom: (() -> Unit)?
) {
    val expandedGroups = remember {
        mutableStateMapOf<Int, Boolean>()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CardPadding)
    ) {
        HeaderSection(
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
            ColumnHeadersRow()
            Spacer(modifier = Modifier.height(8.dp))
            state.items.forEachIndexed { index, item ->
                val percentage =
                    if (state.maxViewsForBar > 0) {
                        item.views.toFloat() /
                            state.maxViewsForBar.toFloat()
                    } else {
                        0f
                    }
                val isExpandable = item.tags.size > 1
                val isExpanded =
                    expandedGroups[index] == true

                TagGroupRow(
                    item = item,
                    percentage = percentage,
                    isExpandable = isExpandable,
                    isExpanded = isExpanded,
                    onClick = if (isExpandable) {
                        {
                            expandedGroups[index] =
                                !isExpanded
                        }
                    } else {
                        null
                    }
                )
                if (isExpandable) {
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        ExpandedTagsSection(
                            tags = item.tags
                        )
                    }
                }
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
private fun HeaderSection(
    onRemoveCard: () -> Unit,
    cardPosition: CardPosition?,
    onMoveUp: (() -> Unit)?,
    onMoveToTop: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onMoveToBottom: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(
                R.string
                    .stats_insights_tags_and_categories
            ),
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
private fun ColumnHeadersRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(
                R.string
                    .stats_insights_tags_and_categories
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.stats_views),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant
        )
    }
}

@Composable
private fun TagGroupRow(
    item: TagGroupUiItem,
    percentage: Float,
    isExpandable: Boolean,
    isExpanded: Boolean,
    onClick: (() -> Unit)?
) {
    val barColor = MaterialTheme.colorScheme.primary
        .copy(alpha = BAR_BACKGROUND_ALPHA)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = percentage)
                .fillMaxHeight()
                .background(barColor)
        )

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
                TagTypeIcon(
                    displayType = item.displayType
                )
                Spacer(
                    modifier = Modifier.width(8.dp)
                )
                if (isExpandable) {
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Default
                                .KeyboardArrowUp
                        } else {
                            Icons.Default
                                .KeyboardArrowDown
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme
                            .onSurfaceVariant
                    )
                    Spacer(
                        modifier = Modifier.width(4.dp)
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
private fun TagTypeIcon(
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
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme
            .onSurfaceVariant
    )
}

@Composable
private fun ExpandedTagsSection(
    tags: List<TagUiItem>
) {
    val lineColor = MaterialTheme.colorScheme.primary
        .copy(alpha = VERTICAL_LINE_ALPHA)

    Column(
        modifier = Modifier.padding(
            start = 24.dp,
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
                    displayType = TagGroupDisplayType
                        .fromTags(listOf(tag))
                )
                Spacer(
                    modifier = Modifier.width(8.dp)
                )
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography
                        .bodyMedium,
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

@Composable
private fun EmptyStateContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(
                R.string.stats_no_data_yet
            ),
            style = MaterialTheme.typography
                .bodyMedium,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun ErrorContent(
    state: TagsAndCategoriesCardUiState.Error,
    onRetry: () -> Unit,
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
        HeaderSection(
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
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text = state.message,
                style = MaterialTheme.typography
                    .bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(
                    text = stringResource(
                        R.string.retry
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TagsAndCategoriesCardLoadingPreview() {
    AppThemeM3 {
        TagsAndCategoriesCard(
            uiState = TagsAndCategoriesCardUiState
                .Loading,
            onShowAllClick = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TagsAndCategoriesCardLoadedPreview() {
    AppThemeM3 {
        TagsAndCategoriesCard(
            uiState = TagsAndCategoriesCardUiState
                .Loaded(
                    items = listOf(
                        TagGroupUiItem(
                            name = "Uncategorized",
                            tags = listOf(
                                TagUiItem(
                                    name =
                                        "Uncategorized",
                                    tagType = "category"
                                )
                            ),
                            views = 83,
                            displayType =
                                TagGroupDisplayType
                                    .CATEGORY
                        ),
                        TagGroupUiItem(
                            name = "snaps",
                            tags = listOf(
                                TagUiItem(
                                    name = "snaps",
                                    tagType = "tag"
                                )
                            ),
                            views = 15,
                            displayType =
                                TagGroupDisplayType.TAG
                        ),
                        TagGroupUiItem(
                            name = "swiftui / stats" +
                                " / jetpack / ios",
                            tags = listOf(
                                TagUiItem(
                                    name = "swiftui",
                                    tagType = "tag"
                                ),
                                TagUiItem(
                                    name = "stats",
                                    tagType = "tag"
                                ),
                                TagUiItem(
                                    name = "jetpack",
                                    tagType = "tag"
                                ),
                                TagUiItem(
                                    name = "ios",
                                    tagType = "tag"
                                )
                            ),
                            views = 1,
                            displayType =
                                TagGroupDisplayType.TAG
                        )
                    ),
                    maxViewsForBar = 83
                ),
            onShowAllClick = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TagsAndCategoriesCardErrorPreview() {
    AppThemeM3 {
        TagsAndCategoriesCard(
            uiState = TagsAndCategoriesCardUiState
                .Error(message = "Failed to load data"),
            onShowAllClick = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}
