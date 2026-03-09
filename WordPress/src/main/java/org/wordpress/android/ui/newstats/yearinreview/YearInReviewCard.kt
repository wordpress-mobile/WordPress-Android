package org.wordpress.android.ui.newstats.yearinreview

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.newstats.components.CardPosition
import org.wordpress.android.ui.newstats.components.StatsCardMenu
import org.wordpress.android.ui.newstats.util.formatStatValue

private val CardCornerRadius = 10.dp
private val CardPadding = 16.dp
private val CardMargin = 16.dp

@Composable
fun YearInReviewCard(
    uiState: YearInReviewCardUiState,
    onRemoveCard: () -> Unit,
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
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant
            .copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surfaceVariant
            .copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant
            .copy(alpha = 0.3f)
    )

    val transition =
        rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation - 500f, 0f),
        end = Offset(translateAnimation, 0f)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CardPadding)
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(16.dp))
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.weight(1f))
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
        Spacer(modifier = Modifier.height(12.dp))
        // Column headers
        MetricHeaderRow()
        state.years.forEachIndexed { index, year ->
            if (index > 0) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme
                        .outlineVariant.copy(alpha = 0.5f)
                )
            }
            YearRow(year)
        }
    }
}

@Composable
private fun MetricHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "",
            modifier = Modifier.weight(1f)
        )
        MetricHeaderLabel(
            stringResource(R.string.stats_insights_posts)
        )
        MetricHeaderLabel(
            stringResource(R.string.stats_insights_total_words)
        )
        MetricHeaderLabel(
            stringResource(R.string.stats_insights_total_likes)
        )
        MetricHeaderLabel(
            stringResource(R.string.stats_comments)
        )
    }
}

@Composable
private fun MetricHeaderLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.width(60.dp),
        maxLines = 1
    )
}

@Composable
private fun YearRow(year: YearSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = year.year,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        MetricValue(formatStatValue(year.totalPosts))
        MetricValue(formatStatValue(year.totalWords))
        MetricValue(formatStatValue(year.totalLikes))
        MetricValue(formatStatValue(year.totalComments))
    }
}

@Composable
private fun MetricValue(value: String) {
    Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.width(60.dp),
        maxLines = 1
    )
}

@Suppress("LongParameterList")
@Composable
private fun ErrorContent(
    state: YearInReviewCardUiState.Error,
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
            Button(onClick = state.onRetry) {
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
            onRemoveCard = {}
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
                    ),
                    YearSummary(
                        year = "2024",
                        totalPosts = 38,
                        totalWords = 12500,
                        avgWords = 328.9,
                        totalLikes = 180,
                        avgLikes = 4.7,
                        totalComments = 60,
                        avgComments = 1.6
                    )
                )
            ),
            onRemoveCard = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun YearInReviewCardErrorPreview() {
    AppThemeM3 {
        YearInReviewCard(
            uiState = YearInReviewCardUiState.Error(
                message = "Failed to load stats",
                onRetry = {}
            ),
            onRemoveCard = {}
        )
    }
}
