package org.wordpress.android.ui.newstats.utm

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.components.CardPosition
import org.wordpress.android.ui.newstats.components.ShowAllFooter
import org.wordpress.android.ui.newstats.components.StatsCardContainer
import org.wordpress.android.ui.newstats.components.StatsCardEmptyContent
import org.wordpress.android.ui.newstats.components.StatsCardErrorContent
import org.wordpress.android.ui.newstats.components.StatsCardHeader
import org.wordpress.android.ui.newstats.components.StatsListHeader
import org.wordpress.android.ui.newstats.util.ShimmerBox

private val CardPadding = 16.dp
private const val LOADING_ITEM_COUNT = 4

@Suppress("LongParameterList")
@Composable
fun UtmCard(
    uiState: UtmCardUiState,
    selectedCategory: UtmCategory,
    onCategoryChanged: (UtmCategory) -> Unit,
    onShowAllClick: () -> Unit,
    onRetry: () -> Unit,
    onRemoveCard: () -> Unit,
    modifier: Modifier = Modifier,
    cardPosition: CardPosition? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveToTop: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onMoveToBottom: (() -> Unit)? = null,
    onOpenWpAdmin: (() -> Unit)? = null
) {
    StatsCardContainer(modifier = modifier) {
        when (uiState) {
            is UtmCardUiState.Loading -> LoadingContent(
                selectedCategory = selectedCategory,
                onCategoryChanged = onCategoryChanged,
                onRemoveCard = onRemoveCard,
                cardPosition = cardPosition,
                onMoveUp = onMoveUp,
                onMoveToTop = onMoveToTop,
                onMoveDown = onMoveDown,
                onMoveToBottom = onMoveToBottom
            )
            is UtmCardUiState.Loaded -> LoadedContent(
                state = uiState,
                selectedCategory = selectedCategory,
                onCategoryChanged = onCategoryChanged,
                onShowAllClick = onShowAllClick,
                onRemoveCard = onRemoveCard,
                cardPosition = cardPosition,
                onMoveUp = onMoveUp,
                onMoveToTop = onMoveToTop,
                onMoveDown = onMoveDown,
                onMoveToBottom = onMoveToBottom
            )
            is UtmCardUiState.Error ->
                StatsCardErrorContent(
                    titleResId = R.string.stats_utm_title,
                    errorMessageResId = uiState.messageResId,
                    onRetry = onRetry,
                    onRemoveCard = onRemoveCard,
                    cardPosition = cardPosition,
                    onMoveUp = onMoveUp,
                    onMoveToTop = onMoveToTop,
                    onMoveDown = onMoveDown,
                    onMoveToBottom = onMoveToBottom,
                    onOpenWpAdmin = onOpenWpAdmin,
                    headerExtra = {
                        UtmCategoryDropdown(
                            selected = selectedCategory,
                            onSelected = onCategoryChanged
                        )
                    }
                )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun LoadingContent(
    selectedCategory: UtmCategory,
    onCategoryChanged: (UtmCategory) -> Unit,
    onRemoveCard: () -> Unit,
    cardPosition: CardPosition?,
    onMoveUp: (() -> Unit)?,
    onMoveToTop: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onMoveToBottom: (() -> Unit)?
) {
    Column(modifier = Modifier.padding(CardPadding)) {
        StatsCardHeader(
            titleResId = R.string.stats_utm_title,
            onRemoveCard = onRemoveCard,
            cardPosition = cardPosition,
            onMoveUp = onMoveUp,
            onMoveToTop = onMoveToTop,
            onMoveDown = onMoveDown,
            onMoveToBottom = onMoveToBottom
        )
        Spacer(modifier = Modifier.height(8.dp))
        UtmCategoryDropdown(
            selected = selectedCategory,
            onSelected = onCategoryChanged
        )
        Spacer(modifier = Modifier.height(12.dp))
        StatsListHeader(
            leftHeaderResId =
                selectedCategory.labelResId
        )
        Spacer(modifier = Modifier.height(8.dp))
        repeat(LOADING_ITEM_COUNT) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                ShimmerBox(
                    modifier = Modifier
                        .width(50.dp)
                        .height(16.dp)
                )
            }
            if (index < LOADING_ITEM_COUNT - 1) {
                Spacer(
                    modifier = Modifier.height(4.dp)
                )
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun LoadedContent(
    state: UtmCardUiState.Loaded,
    selectedCategory: UtmCategory,
    onCategoryChanged: (UtmCategory) -> Unit,
    onShowAllClick: () -> Unit,
    onRemoveCard: () -> Unit,
    cardPosition: CardPosition?,
    onMoveUp: (() -> Unit)?,
    onMoveToTop: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onMoveToBottom: (() -> Unit)?
) {
    Column(modifier = Modifier.padding(CardPadding)) {
        StatsCardHeader(
            titleResId = R.string.stats_utm_title,
            onRemoveCard = onRemoveCard,
            cardPosition = cardPosition,
            onMoveUp = onMoveUp,
            onMoveToTop = onMoveToTop,
            onMoveDown = onMoveDown,
            onMoveToBottom = onMoveToBottom
        )
        Spacer(modifier = Modifier.height(8.dp))
        UtmCategoryDropdown(
            selected = selectedCategory,
            onSelected = onCategoryChanged
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (state.items.isEmpty()) {
            StatsCardEmptyContent()
        } else {
            StatsListHeader(
                leftHeaderResId =
                    selectedCategory.labelResId
            )
            Spacer(modifier = Modifier.height(8.dp))

            state.items.forEachIndexed { index, item ->
                val percentage =
                    if (state.maxViewsForBar > 0) {
                        item.views.toFloat() /
                            state.maxViewsForBar.toFloat()
                    } else {
                        0f
                    }
                UtmExpandableRow(
                    item = item,
                    percentage = percentage,
                    maxViewsForBar =
                        state.maxViewsForBar
                )
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
private fun UtmCategoryDropdown(
    selected: UtmCategory,
    onSelected: (UtmCategory) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clickable { expanded = true },
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    selected.labelResId
                ),
                style = MaterialTheme.typography
                    .labelLarge,
                color = MaterialTheme.colorScheme
                    .onSurface
            )
            Icon(
                imageVector =
                    Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme
                    .onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            UtmCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                category.labelResId
                            )
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(category)
                    }
                )
            }
        }
    }
}
