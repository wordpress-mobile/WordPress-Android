package org.wordpress.android.ui.newstats.weeklyviews

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.shader.verticalGradient
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import java.util.Locale
import kotlin.math.abs

private val CardCornerRadius = 10.dp
private val CardPadding = 16.dp
private val CardMargin = 16.dp
private val ChartHeight = 120.dp
private val StatItemWidth = 100.dp
private val BadgeCornerRadius = 4.dp
private const val THOUSAND = 1_000
private const val MILLION = 1_000_000
private const val CHANGE_BADGE_POSITIVE_COLOR = 0xFF4CAF50
private const val CHANGE_BADGE_NEGATIVE_COLOR = 0xFFE91E63

@Composable
fun WeeklyViewsCard(
    uiState: WeeklyViewsCardUiState,
    modifier: Modifier = Modifier
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
            is WeeklyViewsCardUiState.Loading -> LoadingContent()
            is WeeklyViewsCardUiState.Loaded -> LoadedContent(uiState)
            is WeeklyViewsCardUiState.Error -> ErrorContent(uiState)
        }
    }
}

@Composable
private fun LoadingContent() {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation.value - 500f, 0f),
        end = Offset(translateAnimation.value, 0f)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CardPadding)
    ) {
        // Header shimmer
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Column {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Chart shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChartHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Bottom stats shimmer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(60.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
        }
    }
}

@Composable
private fun LoadedContent(state: WeeklyViewsCardUiState.Loaded) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CardPadding)
    ) {
        // Header Section
        HeaderSection(state)
        Spacer(modifier = Modifier.height(16.dp))
        // Chart Section
        WeeklyStatsChart(
            chartData = state.chartData,
            weeklyAverage = state.weeklyAverage
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Bottom Stats Row
        BottomStatsRow(stats = state.bottomStats)
    }
}

@Composable
private fun HeaderSection(state: WeeklyViewsCardUiState.Loaded) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.stats_views),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: Current and previous week totals with difference
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatStatValue(state.currentWeekViews),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatStatValue(state.previousWeekViews),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                DifferenceRow(
                    difference = state.viewsDifference,
                    percentageChange = state.viewsPercentageChange
                )
            }
            // Right: Date ranges with colored dots
            Column(horizontalAlignment = Alignment.End) {
                DateRangeWithDot(
                    dateRange = state.currentWeekDateRange,
                    dotColor = MaterialTheme.colorScheme.primary,
                    isFilled = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                DateRangeWithDot(
                    dateRange = state.previousWeekDateRange,
                    dotColor = MaterialTheme.colorScheme.outline,
                    isFilled = false
                )
            }
        }
    }
}

@Composable
private fun DifferenceRow(difference: Long, percentageChange: Double) {
    val isNegative = difference < 0
    val arrowText = if (isNegative) "↘" else if (difference > 0) "↗" else "↔"
    val color = when {
        difference < 0 -> MaterialTheme.colorScheme.error
        difference > 0 -> Color(CHANGE_BADGE_POSITIVE_COLOR)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = formatDifference(difference),
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = arrowText,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = String.format(Locale.getDefault(), "%.1f%%", abs(percentageChange)),
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

@Composable
private fun DateRangeWithDot(dateRange: String, dotColor: Color, isFilled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = dateRange,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(8.dp)
                .then(
                    if (isFilled) {
                        Modifier
                            .clip(CircleShape)
                            .background(dotColor)
                    } else {
                        Modifier
                            .clip(CircleShape)
                            .border(1.5.dp, dotColor, CircleShape)
                    }
                )
        )
    }
}

@Suppress("UnusedParameter")
@Composable
private fun WeeklyStatsChart(chartData: WeeklyChartData, weeklyAverage: Long) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val hasPreviousWeek = chartData.previousWeek.isNotEmpty()

    LaunchedEffect(chartData) {
        if (chartData.currentWeek.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    series(chartData.currentWeek.map { it.views.toInt() })
                    if (hasPreviousWeek) {
                        series(chartData.previousWeek.map { it.views.toInt() })
                    }
                }
            }
        }
    }

    if (chartData.currentWeek.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChartHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.stats_no_data_yet),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    val areaGradient = ShaderProvider.verticalGradient(
        colors = arrayOf(
            primaryColor.copy(alpha = 0.4f),
            primaryColor.copy(alpha = 0f)
        )
    )

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.Line(
                        fill = LineCartesianLayer.LineFill.single(fill(primaryColor)),
                        areaFill = LineCartesianLayer.AreaFill.single(fill(areaGradient)),
                        pointConnector = LineCartesianLayer.PointConnector.cubic()
                    ),
                    LineCartesianLayer.Line(
                        fill = LineCartesianLayer.LineFill.single(fill(secondaryColor)),
                        stroke = LineCartesianLayer.LineStroke.Dashed(),
                        pointConnector = LineCartesianLayer.PointConnector.cubic()
                    )
                )
            )
        ),
        modelProducer = modelProducer,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        modifier = Modifier
            .fillMaxWidth()
            .height(ChartHeight)
    )
}

@Composable
private fun BottomStatsRow(stats: List<StatItem>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        items(stats) { stat ->
            StatItemCard(stat)
        }
    }
}

@Composable
private fun StatItemCard(stat: StatItem) {
    val icon = when (stat.label) {
        stringResource(R.string.stats_views) -> Icons.Default.Visibility
        stringResource(R.string.stats_visitors) -> Icons.Default.Person
        stringResource(R.string.stats_likes) -> Icons.Default.FavoriteBorder
        stringResource(R.string.stats_comments) -> Icons.Default.ChatBubbleOutline
        stringResource(R.string.posts) -> Icons.Default.Edit
        else -> Icons.Default.Visibility
    }

    Column(
        modifier = Modifier.width(StatItemWidth),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stat.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatStatValue(stat.value),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        ChangeBadge(change = stat.change)
    }
}

@Composable
private fun ChangeBadge(change: StatChange) {
    val (text, backgroundColor, textColor) = when (change) {
        is StatChange.Positive -> Triple(
            "↗ ${String.format(Locale.getDefault(), "%.1f%%", change.percentage)}",
            Color(CHANGE_BADGE_POSITIVE_COLOR).copy(alpha = 0.15f),
            Color(CHANGE_BADGE_POSITIVE_COLOR)
        )
        is StatChange.Negative -> Triple(
            "↘ ${String.format(Locale.getDefault(), "%.1f%%", change.percentage)}",
            Color(CHANGE_BADGE_NEGATIVE_COLOR).copy(alpha = 0.15f),
            Color(CHANGE_BADGE_NEGATIVE_COLOR)
        )
        is StatChange.NoChange -> Triple(
            "↔ 0%",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BadgeCornerRadius))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ErrorContent(state: WeeklyViewsCardUiState.Error) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CardPadding)
    ) {
        Text(
            text = stringResource(R.string.stats_views),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChartHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.stats_no_data_yet),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}

private fun formatStatValue(value: Long): String {
    return when {
        value >= MILLION -> String.format(Locale.getDefault(), "%.1fM", value / MILLION.toDouble())
        value >= THOUSAND -> String.format(Locale.getDefault(), "%.1fK", value / THOUSAND.toDouble())
        else -> value.toString()
    }
}

private fun formatDifference(difference: Long): String {
    val absValue = abs(difference)
    val formattedValue = when {
        absValue >= MILLION -> String.format(
            Locale.getDefault(),
            "%.1fM",
            absValue / MILLION.toDouble()
        )
        absValue >= THOUSAND -> String.format(
            Locale.getDefault(),
            "%.1fK",
            absValue / THOUSAND.toDouble()
        )
        else -> absValue.toString()
    }
    return if (difference < 0) "-$formattedValue" else "+$formattedValue"
}

@Preview(showBackground = true)
@Composable
private fun WeeklyViewsCardLoadingPreview() {
    AppThemeM3 {
        WeeklyViewsCard(uiState = WeeklyViewsCardUiState.Loading)
    }
}

@Preview(showBackground = true)
@Composable
private fun WeeklyViewsCardLoadedPreview() {
    AppThemeM3 {
        WeeklyViewsCard(
            uiState = WeeklyViewsCardUiState.Loaded(
                currentWeekViews = 7467,
                previousWeekViews = 8289,
                viewsDifference = -822,
                viewsPercentageChange = -9.9,
                currentWeekDateRange = "14-20 Jan",
                previousWeekDateRange = "7-13 Jan",
                chartData = WeeklyChartData(
                    currentWeek = listOf(
                        DailyDataPoint("Jan 14", 800),
                        DailyDataPoint("Jan 15", 1200),
                        DailyDataPoint("Jan 16", 950),
                        DailyDataPoint("Jan 17", 1100),
                        DailyDataPoint("Jan 18", 1300),
                        DailyDataPoint("Jan 19", 1017),
                        DailyDataPoint("Jan 20", 1100)
                    ),
                    previousWeek = listOf(
                        DailyDataPoint("Jan 7", 1000),
                        DailyDataPoint("Jan 8", 1400),
                        DailyDataPoint("Jan 9", 1150),
                        DailyDataPoint("Jan 10", 1200),
                        DailyDataPoint("Jan 11", 1350),
                        DailyDataPoint("Jan 12", 1089),
                        DailyDataPoint("Jan 13", 1100)
                    )
                ),
                weeklyAverage = 1066,
                bottomStats = listOf(
                    StatItem("Views", 7467, StatChange.Negative(9.9)),
                    StatItem("Visitors", 2000, StatChange.Negative(5.6)),
                    StatItem("Likes", 0, StatChange.NoChange),
                    StatItem("Comments", 0, StatChange.NoChange),
                    StatItem("Posts", 5, StatChange.Positive(25.0))
                )
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WeeklyViewsCardErrorPreview() {
    AppThemeM3 {
        WeeklyViewsCard(
            uiState = WeeklyViewsCardUiState.Error(
                message = "Failed to load weekly stats",
                onRetry = {}
            )
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WeeklyViewsCardLoadedDarkPreview() {
    AppThemeM3 {
        WeeklyViewsCard(
            uiState = WeeklyViewsCardUiState.Loaded(
                currentWeekViews = 7467,
                previousWeekViews = 8289,
                viewsDifference = -822,
                viewsPercentageChange = -9.9,
                currentWeekDateRange = "14-20 Jan",
                previousWeekDateRange = "7-13 Jan",
                chartData = WeeklyChartData(
                    currentWeek = listOf(
                        DailyDataPoint("Jan 14", 800),
                        DailyDataPoint("Jan 15", 1200),
                        DailyDataPoint("Jan 16", 950),
                        DailyDataPoint("Jan 17", 1100),
                        DailyDataPoint("Jan 18", 1300),
                        DailyDataPoint("Jan 19", 1017),
                        DailyDataPoint("Jan 20", 1100)
                    ),
                    previousWeek = listOf(
                        DailyDataPoint("Jan 7", 1000),
                        DailyDataPoint("Jan 8", 1400),
                        DailyDataPoint("Jan 9", 1150),
                        DailyDataPoint("Jan 10", 1200),
                        DailyDataPoint("Jan 11", 1350),
                        DailyDataPoint("Jan 12", 1089),
                        DailyDataPoint("Jan 13", 1100)
                    )
                ),
                weeklyAverage = 1066,
                bottomStats = listOf(
                    StatItem("Views", 7467, StatChange.Negative(9.9)),
                    StatItem("Visitors", 2000, StatChange.Negative(5.6)),
                    StatItem("Likes", 0, StatChange.NoChange),
                    StatItem("Comments", 0, StatChange.NoChange),
                    StatItem("Posts", 5, StatChange.Positive(25.0))
                )
            )
        )
    }
}
