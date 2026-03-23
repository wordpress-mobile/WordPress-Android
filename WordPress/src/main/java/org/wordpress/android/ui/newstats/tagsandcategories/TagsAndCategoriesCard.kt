package org.wordpress.android.ui.newstats.tagsandcategories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.newstats.components.CardPosition
import org.wordpress.android.ui.newstats.components.ShowAllFooter
import org.wordpress.android.ui.newstats.components.StatsCardContainer
import org.wordpress.android.ui.newstats.components.StatsCardEmptyContent
import org.wordpress.android.ui.newstats.components.StatsCardHeader
import org.wordpress.android.ui.newstats.components.StatsListHeader
import org.wordpress.android.ui.newstats.util.ShimmerBox

private val CardPadding = 16.dp
private const val LOADING_SHIMMER_ITEM_COUNT = 5

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
    StatsCardContainer(modifier = modifier) {
        when (uiState) {
            is TagsAndCategoriesCardUiState.Loading ->
                LoadingContent()
            is TagsAndCategoriesCardUiState.NoData ->
                NoDataContent(
                    onRemoveCard,
                    cardPosition,
                    onMoveUp,
                    onMoveToTop,
                    onMoveDown,
                    onMoveToBottom
                )
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
private fun NoDataContent(
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
                R.string
                    .stats_insights_tags_and_categories,
            onRemoveCard = onRemoveCard,
            cardPosition = cardPosition,
            onMoveUp = onMoveUp,
            onMoveToTop = onMoveToTop,
            onMoveDown = onMoveDown,
            onMoveToBottom = onMoveToBottom
        )
        Spacer(modifier = Modifier.height(12.dp))
        StatsCardEmptyContent()
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
    val expandedGroups = remember(state.items) {
        mutableStateMapOf<Int, Boolean>()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CardPadding)
    ) {
        StatsCardHeader(
            titleResId =
                R.string
                    .stats_insights_tags_and_categories,
            onRemoveCard = onRemoveCard,
            cardPosition = cardPosition,
            onMoveUp = onMoveUp,
            onMoveToTop = onMoveToTop,
            onMoveDown = onMoveDown,
            onMoveToBottom = onMoveToBottom
        )
        Spacer(modifier = Modifier.height(12.dp))
        StatsListHeader(
            leftHeaderResId =
                R.string
                    .stats_insights_tags_and_categories,
            rightHeaderResId =
                R.string.stats_views
        )
        Spacer(modifier = Modifier.height(8.dp))
        state.items.forEachIndexed { index, item ->
            val percentage =
                if (state.maxViewsForBar > 0) {
                    item.views.toFloat() /
                        state.maxViewsForBar
                            .toFloat()
                } else {
                    0f
                }
            val isExpanded =
                expandedGroups[index] == true

            TagGroupRow(
                item = item,
                percentage = percentage,
                isExpandable = item.isExpandable,
                isExpanded = isExpanded,
                onClick = if (item.isExpandable) {
                    {
                        expandedGroups[index] =
                            !isExpanded
                    }
                } else {
                    null
                }
            )
            if (item.isExpandable) {
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
                    modifier =
                        Modifier.height(4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        ShowAllFooter(onClick = onShowAllClick)
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
        StatsCardHeader(
            titleResId =
                R.string
                    .stats_insights_tags_and_categories,
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
                color = MaterialTheme.colorScheme
                    .error
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
                .Error(
                    message = "Failed to load data"
                ),
            onShowAllClick = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}
