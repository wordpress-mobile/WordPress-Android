package org.wordpress.android.ui.newstats.yearinreview

import androidx.annotation.StringRes
import org.wordpress.android.ui.newstats.util.rememberShimmerBrush
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.newstats.components.CardPosition
import org.wordpress.android.ui.newstats.components.ShowAllFooter
import org.wordpress.android.ui.newstats.components.StatsCardMenu
import org.wordpress.android.ui.newstats.util.formatStatValue

private val CardCornerRadius = 10.dp
private val CardPadding = 16.dp
private val CardMargin = 16.dp
private val MiniCardCornerRadius = 8.dp

@Composable
@Suppress("LongParameterList")
fun YearInReviewCard(
    uiState: YearInReviewCardUiState,
    onRemoveCard: () -> Unit,
    onShowAllClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    cardPosition: CardPosition? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveToTop: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onMoveToBottom: (() -> Unit)? = null
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
            is YearInReviewCardUiState.Loading ->
                LoadingContent()
            is YearInReviewCardUiState.Loaded ->
                LoadedContent(
                    uiState,
                    onRemoveCard,
                    onShowAllClick,
                    cardPosition,
                    onMoveUp,
                    onMoveToTop,
                    onMoveDown,
                    onMoveToBottom
                )
            is YearInReviewCardUiState.Error ->
                ErrorContent(
                    uiState,
                    onRemoveCard,
                    onRetry,
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
    val shimmerBrush = rememberShimmerBrush()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CardPadding)
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(16.dp))
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp)
                            .clip(
                                RoundedCornerShape(
                                    MiniCardCornerRadius
                                )
                            )
                            .background(shimmerBrush)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun LoadedContent(
    state: YearInReviewCardUiState.Loaded,
    onRemoveCard: () -> Unit,
    onShowAllClick: () -> Unit,
    cardPosition: CardPosition?,
    onMoveUp: (() -> Unit)?,
    onMoveToTop: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onMoveToBottom: (() -> Unit)?
) {
    val year = state.years.firstOrNull() ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CardPadding)
    ) {
        // Title row: "2023 in review" + menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    R.string
                        .stats_insights_year_in_review_title,
                    year.year
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
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
        Spacer(modifier = Modifier.height(12.dp))
        // 2x2 grid of mini stat cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            StatMiniCard(
                icon = Icons.AutoMirrored.Outlined.Article,
                labelRes = R.string.stats_insights_posts,
                value = formatStatValue(year.totalPosts),
                modifier = Modifier.weight(1f)
            )
            StatMiniCard(
                icon = Icons.AutoMirrored.Outlined.TextSnippet,
                labelRes =
                    R.string.stats_insights_words,
                value = formatStatValue(year.totalWords),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            StatMiniCard(
                icon = Icons.Outlined.StarOutline,
                labelRes =
                    R.string.stats_insights_likes,
                value = formatStatValue(year.totalLikes),
                modifier = Modifier.weight(1f)
            )
            StatMiniCard(
                icon = Icons.AutoMirrored.Outlined.Chat,
                labelRes = R.string.stats_comments,
                value = formatStatValue(
                    year.totalComments
                ),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        ShowAllFooter(onClick = onShowAllClick)
    }
}

@Composable
private fun StatMiniCard(
    icon: ImageVector,
    @StringRes labelRes: Int,
    value: String,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(MiniCardCornerRadius))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(
                    MiniCardCornerRadius
                )
            )
            .background(
                MaterialTheme.colorScheme.surfaceContainerLowest
            )
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun ErrorContent(
    state: YearInReviewCardUiState.Error,
    onRemoveCard: () -> Unit,
    onRetry: () -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    R.string.stats_insights_year_in_review
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
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
                Text(
                    text = stringResource(R.string.retry)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun YearInReviewCardLoadingPreview() {
    AppThemeM3 {
        YearInReviewCard(
            uiState = YearInReviewCardUiState.Loading,
            onRemoveCard = {},
            onShowAllClick = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun YearInReviewCardLoadedPreview() {
    AppThemeM3 {
        YearInReviewCard(
            uiState = YearInReviewCardUiState.Loaded(
                years = listOf(
                    YearSummary(
                        year = "2025",
                        totalPosts = 42,
                        totalWords = 15000,
                        avgWords = 357.1,
                        totalLikes = 230,
                        avgLikes = 5.5,
                        totalComments = 85,
                        avgComments = 2.0
                    )
                )
            ),
            onRemoveCard = {},
            onShowAllClick = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun YearInReviewCardErrorPreview() {
    AppThemeM3 {
        YearInReviewCard(
            uiState = YearInReviewCardUiState.Error(
                message = "Failed to load stats"
            ),
            onRemoveCard = {},
            onShowAllClick = {},
            onRetry = {}
        )
    }
}
