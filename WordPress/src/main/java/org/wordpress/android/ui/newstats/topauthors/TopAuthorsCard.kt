package org.wordpress.android.ui.newstats.topauthors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.newstats.components.CardPosition
import org.wordpress.android.ui.newstats.components.ShowAllFooter
import org.wordpress.android.ui.newstats.components.StatsCardContainer
import org.wordpress.android.ui.newstats.components.StatsCardEmptyContent
import org.wordpress.android.ui.newstats.components.StatsCardErrorContent
import org.wordpress.android.ui.newstats.components.StatsCardHeader
import org.wordpress.android.ui.newstats.components.StatsItemName
import org.wordpress.android.ui.newstats.components.StatsListHeader
import org.wordpress.android.ui.newstats.components.StatsListRowContainer
import org.wordpress.android.ui.newstats.components.StatsViewChange
import org.wordpress.android.ui.newstats.components.StatsViewsColumn
import org.wordpress.android.ui.newstats.util.ShimmerBox

private val CardPadding = 16.dp
private const val LOADING_ITEM_COUNT = 4

@Composable
fun TopAuthorsCard(
    uiState: TopAuthorsCardUiState,
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
            is TopAuthorsCardUiState.Loading -> LoadingContent()
            is TopAuthorsCardUiState.Loaded -> LoadedContent(
                uiState, onShowAllClick, onRemoveCard,
                cardPosition, onMoveUp, onMoveToTop, onMoveDown, onMoveToBottom
            )
            is TopAuthorsCardUiState.Error -> StatsCardErrorContent(
                titleResId = R.string.stats_authors_title,
                errorMessage = uiState.message,
                onRetry = onRetry,
                onRemoveCard = onRemoveCard,
                cardPosition = cardPosition,
                onMoveUp = onMoveUp,
                onMoveToTop = onMoveToTop,
                onMoveDown = onMoveDown,
                onMoveToBottom = onMoveToBottom
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(modifier = Modifier.padding(CardPadding)) {
        // Title placeholder
        ShimmerBox(
            modifier = Modifier
                .width(100.dp)
                .height(20.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // List items placeholders
        repeat(LOADING_ITEM_COUNT) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
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
        }
    }
}

@Composable
private fun LoadedContent(
    state: TopAuthorsCardUiState.Loaded,
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
            titleResId = R.string.stats_authors_title,
            onRemoveCard = onRemoveCard,
            cardPosition = cardPosition,
            onMoveUp = onMoveUp,
            onMoveToTop = onMoveToTop,
            onMoveDown = onMoveDown,
            onMoveToBottom = onMoveToBottom
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (state.authors.isEmpty()) {
            StatsCardEmptyContent()
        } else {
            StatsListHeader(leftHeaderResId = R.string.stats_authors_author_header)
            Spacer(modifier = Modifier.height(8.dp))

            // Author list (capped at 10 items)
            state.authors.forEachIndexed { index, author ->
                val percentage = if (state.maxViewsForBar > 0) {
                    author.views.toFloat() / state.maxViewsForBar.toFloat()
                } else 0f
                AuthorRow(author = author, percentage = percentage)
                if (index < state.authors.lastIndex) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Show All footer
            Spacer(modifier = Modifier.height(12.dp))
            ShowAllFooter(onClick = onShowAllClick)
        }
    }
}

@Composable
private fun AuthorRow(
    author: TopAuthorUiItem,
    percentage: Float
) {
    StatsListRowContainer(percentage = percentage) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuthorAvatar(avatarUrl = author.avatarUrl, name = author.name)
            Spacer(modifier = Modifier.width(12.dp))
            StatsItemName(name = author.name, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(12.dp))
            StatsViewsColumn(views = author.views, change = author.change)
        }
    }
}

@Composable
fun AuthorAvatar(
    avatarUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Int = 40
) {
    if (avatarUrl != null) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = name,
            modifier = modifier
                .size(size.dp)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size((size * 0.6).dp)
            )
        }
    }
}

// Previews
@Preview(showBackground = true)
@Composable
private fun TopAuthorsCardLoadingPreview() {
    AppThemeM3 {
        TopAuthorsCard(
            uiState = TopAuthorsCardUiState.Loading,
            onShowAllClick = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TopAuthorsCardLoadedPreview() {
    AppThemeM3 {
        TopAuthorsCard(
            uiState = TopAuthorsCardUiState.Loaded(
                authors = listOf(
                    TopAuthorUiItem("John Doe", null, 3464, StatsViewChange.Positive(124, 3.7)),
                    TopAuthorUiItem("Jane Smith", null, 556, StatsViewChange.Positive(45, 8.8)),
                    TopAuthorUiItem("Bob Johnson", null, 522, StatsViewChange.Negative(12, 2.2)),
                    TopAuthorUiItem("Alice Brown", null, 485, StatsViewChange.NoChange)
                ),
                maxViewsForBar = 3464,
                hasMoreItems = true
            ),
            onShowAllClick = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TopAuthorsCardErrorPreview() {
    AppThemeM3 {
        TopAuthorsCard(
            uiState = TopAuthorsCardUiState.Error("Failed to load author data"),
            onShowAllClick = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}
