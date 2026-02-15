package org.wordpress.android.ui.newstats.viewsstats

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
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.shader.verticalGradient
import com.patrykandpatrick.vico.core.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.core.common.Insets
import com.patrykandpatrick.vico.core.common.component.LineComponent
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.newstats.components.CardPosition
import org.wordpress.android.ui.newstats.components.StatsCardMenu
import org.wordpress.android.ui.newstats.util.formatStatValue
import java.util.Locale
import kotlin.math.abs

private val CardCornerRadius = 10.dp
private val CardPadding = 16.dp
private val CardMargin = 16.dp
private val ChartHeight = 120.dp
private val StatItemWidth = 100.dp
private val BadgeCornerRadius = 4.dp
private val ChangeBadgePositiveColor = Color(0xFF4CAF50)
private val ChangeBadgeNegativeColor = Color(0xFFE91E63)

// Preview sample data constants
private const val SAMPLE_CURRENT_VIEWS = 7467L
private const val SAMPLE_PREVIOUS_VIEWS = 8289L
private const val SAMPLE_VIEWS_DIFFERENCE = -822L
private const val SAMPLE_VIEWS_PERCENTAGE = -9.9
private const val SAMPLE_VISITORS = 2000L
private const val SAMPLE_VISITORS_PERCENTAGE = 5.6
private const val SAMPLE_POSTS = 5L
private const val SAMPLE_POSTS_PERCENTAGE = 25.0
private const val SAMPLE_PERIOD_AVERAGE = 1066L
private val SAMPLE_CURRENT_PERIOD_DATA = listOf(800L, 1200L, 950L, 1100L, 1300L, 1017L, 1100L)
private val SAMPLE_PREVIOUS_PERIOD_DATA = listOf(1000L, 1400L, 1150L, 1200L, 1350L, 1089L, 1100L)

@Composable
fun ViewsStatsCard(
    uiState: ViewsStatsCardUiState,
    onChartTypeChanged: (ChartType) -> Unit,
    onRetry: () -> Unit,
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
            is ViewsStatsCardUiState.Loading -> LoadingContent()
            is ViewsStatsCardUiState.Loaded -> LoadedContent(
                uiState, onChartTypeChanged, onRemoveCard,
                cardPosition, onMoveUp, onMoveToTop, onMoveDown, onMoveToBottom
            )
            is ViewsStatsCardUiState.Error -> ErrorContent(
                uiState, onRetry, onRemoveCard,
                cardPosition, onMoveUp, onMoveToTop, onMoveDown, onMoveToBottom
            )
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
private fun LoadedContent(
    state: ViewsStatsCardUiState.Loaded,
    onChartTypeChanged: (ChartType) -> Unit,
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
        // Header Section
        HeaderSection(
            state = state,
            onChartTypeChanged = onChartTypeChanged,
            onRemoveCard = onRemoveCard,
            cardPosition = cardPosition,
            onMoveUp = onMoveUp,
            onMoveToTop = onMoveToTop,
            onMoveDown = onMoveDown,
            onMoveToBottom = onMoveToBottom
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Chart Section
        ViewsStatsChart(
            chartData = state.chartData,
            periodAverage = state.periodAverage,
            chartType = state.chartType
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Bottom Stats Row
        BottomStatsRow(stats = state.bottomStats)
    }
}

@Composable
private fun HeaderSection(
    state: ViewsStatsCardUiState.Loaded,
    onChartTypeChanged: (ChartType) -> Unit,
    onRemoveCard: () -> Unit,
    cardPosition: CardPosition?,
    onMoveUp: (() -> Unit)?,
    onMoveToTop: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onMoveToBottom: (() -> Unit)?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.stats_views),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            StatsCardMenu(
                onRemoveClick = onRemoveCard,
                cardPosition = cardPosition,
                onMoveUp = onMoveUp,
                onMoveToTop = onMoveToTop,
                onMoveDown = onMoveDown,
                onMoveToBottom = onMoveToBottom,
                additionalContent = {
                    ChartTypeMenuItems(
                        currentChartType = state.chartType,
                        onChartTypeSelected = onChartTypeChanged
                    )
                }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: Current and previous period totals with difference
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatStatValue(state.currentPeriodViews),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatStatValue(state.previousPeriodViews),
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
            // Right: Date ranges with colored dots and average
            Column(horizontalAlignment = Alignment.End) {
                DateRangeWithDot(
                    dateRange = state.currentPeriodDateRange,
                    dotColor = MaterialTheme.colorScheme.primary,
                    isFilled = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                DateRangeWithDot(
                    dateRange = state.previousPeriodDateRange,
                    dotColor = MaterialTheme.colorScheme.outline,
                    isFilled = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                AverageRow(average = state.periodAverage)
            }
        }
    }
}

/**
 * Menu items for chart type selection. Used as additionalContent in StatsCardMenu.
 */
@Composable
private fun ChartTypeMenuItems(
    currentChartType: ChartType,
    onChartTypeSelected: (ChartType) -> Unit
) {
    DropdownMenuItem(
        text = { Text(text = stringResource(R.string.stats_chart_type_lines)) },
        onClick = { onChartTypeSelected(ChartType.LINE) },
        enabled = currentChartType != ChartType.LINE,
        leadingIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                contentDescription = null
            )
        }
    )
    DropdownMenuItem(
        text = { Text(text = stringResource(R.string.stats_chart_type_bars)) },
        onClick = { onChartTypeSelected(ChartType.BAR) },
        enabled = currentChartType != ChartType.BAR,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = null
            )
        }
    )
}

@Composable
private fun DifferenceRow(difference: Long, percentageChange: Double) {
    val isNegative = difference < 0
    val arrowText = if (isNegative) "↘" else if (difference > 0) "↗" else "↔"
    val color = when {
        difference < 0 -> MaterialTheme.colorScheme.error
        difference > 0 -> ChangeBadgePositiveColor
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

@Composable
private fun AverageRow(average: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.stats_weekly_average, formatStatValue(average)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline)
        )
    }
}

@Composable
private fun ViewsStatsChart(
    chartData: ViewsStatsChartData,
    periodAverage: Long,
    chartType: ChartType
) {
    // Key the model producer on chartType so it gets recreated when chart type changes
    val modelProducer = remember(chartType) { CartesianChartModelProducer() }

    // Use both lists as keys to ensure LaunchedEffect re-runs when either changes
    LaunchedEffect(chartData.currentPeriod, chartData.previousPeriod, chartType) {
        if (chartData.currentPeriod.isNotEmpty()) {
            // Check hasPreviousPeriod inside the effect to avoid capturing stale values
            val hasPreviousPeriod = chartData.previousPeriod.isNotEmpty()
            when (chartType) {
                ChartType.LINE -> modelProducer.runTransaction {
                    lineSeries {
                        series(chartData.currentPeriod.map { it.views.toInt() })
                        if (hasPreviousPeriod) {
                            series(chartData.previousPeriod.map { it.views.toInt() })
                        }
                    }
                }
                ChartType.BAR -> modelProducer.runTransaction {
                    columnSeries {
                        // Current period first (primary color)
                        series(chartData.currentPeriod.map { it.views.toInt() })
                        // Previous period second (grey)
                        if (hasPreviousPeriod) {
                            series(chartData.previousPeriod.map { it.views.toInt() })
                        }
                    }
                }
            }
        }
    }

    if (chartData.currentPeriod.isEmpty()) {
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

    // X-axis formatter to show date labels from current period data
    val dateLabels = chartData.currentPeriod.map { it.label }
    val bottomAxisValueFormatter = CartesianValueFormatter { _, value, _ ->
        val index = value.toInt()
        if (index in dateLabels.indices) dateLabels[index] else ""
    }

    // Marker value formatter to show date and views on touch
    val markerValueFormatter = remember(chartData) {
        ChartMarkerValueFormatter(
            currentPeriodData = chartData.currentPeriod,
            previousPeriodData = chartData.previousPeriod
        )
    }

    // Marker that shows on touch
    val marker = rememberDefaultCartesianMarker(
        label = rememberTextComponent(
            color = MaterialTheme.colorScheme.onSurface,
            textSize = 12.sp,
            lineCount = 3,
            padding = Insets(horizontalDp = 12f, verticalDp = 8f),
            background = rememberShapeComponent(
                fill = fill(MaterialTheme.colorScheme.surfaceContainer),
                shape = CorneredShape.rounded(allPercent = 25)
            )
        ),
        guideline = LineComponent(
            fill = fill(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            thicknessDp = 1f
        ),
        valueFormatter = markerValueFormatter
    )

    // Horizontal line for period average
    val averageLine = HorizontalLine(
        y = { periodAverage.toDouble() },
        line = LineComponent(
            fill = fill(MaterialTheme.colorScheme.outline),
            thicknessDp = 1f
        )
    )

    when (chartType) {
        ChartType.LINE -> {
            val areaGradient = ShaderProvider.verticalGradient(
                colors = arrayOf(
                    primaryColor.copy(alpha = 0.8f),
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
                    ),
                    startAxis = VerticalAxis.rememberStart(line = null),
                    bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = bottomAxisValueFormatter),
                    marker = marker,
                    decorations = listOf(averageLine)
                ),
                modelProducer = modelProducer,
                scrollState = rememberVicoScrollState(scrollEnabled = false),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ChartHeight)
            )
        }
        ChartType.BAR -> {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(
                        columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                            // Current period (primary color)
                            LineComponent(
                                fill = fill(primaryColor),
                                thicknessDp = 8f,
                                shape = CorneredShape.rounded(allPercent = 40)
                            ),
                            // Previous period (grey)
                            LineComponent(
                                fill = fill(secondaryColor),
                                thicknessDp = 8f,
                                shape = CorneredShape.rounded(allPercent = 40)
                            )
                        ),
                        mergeMode = { ColumnCartesianLayer.MergeMode.Grouped(4f) }
                    ),
                    startAxis = VerticalAxis.rememberStart(line = null),
                    bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = bottomAxisValueFormatter),
                    marker = marker,
                    decorations = listOf(averageLine)
                ),
                modelProducer = modelProducer,
                scrollState = rememberVicoScrollState(scrollEnabled = false),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ChartHeight)
            )
        }
    }
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
            ChangeBadgePositiveColor.copy(alpha = 0.15f),
            ChangeBadgePositiveColor
        )
        is StatChange.Negative -> Triple(
            "↘ ${String.format(Locale.getDefault(), "%.1f%%", change.percentage)}",
            ChangeBadgeNegativeColor.copy(alpha = 0.15f),
            ChangeBadgeNegativeColor
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
private fun ErrorContent(
    state: ViewsStatsCardUiState.Error,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.stats_views),
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
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}

private fun formatDifference(difference: Long): String {
    val formattedValue = formatStatValue(abs(difference))
    return if (difference < 0) "-$formattedValue" else "+$formattedValue"
}

@Preview(showBackground = true)
@Composable
private fun ViewsStatsCardLoadingPreview() {
    AppThemeM3 {
        ViewsStatsCard(
            uiState = ViewsStatsCardUiState.Loading,
            onChartTypeChanged = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}

private fun sampleLoadedState(): ViewsStatsCardUiState.Loaded {
    val currentPeriodLabels = listOf("Jan 14", "Jan 15", "Jan 16", "Jan 17", "Jan 18", "Jan 19", "Jan 20")
    val previousPeriodLabels = listOf("Jan 7", "Jan 8", "Jan 9", "Jan 10", "Jan 11", "Jan 12", "Jan 13")

    return ViewsStatsCardUiState.Loaded(
        currentPeriodViews = SAMPLE_CURRENT_VIEWS,
        previousPeriodViews = SAMPLE_PREVIOUS_VIEWS,
        viewsDifference = SAMPLE_VIEWS_DIFFERENCE,
        viewsPercentageChange = SAMPLE_VIEWS_PERCENTAGE,
        currentPeriodDateRange = "14-20 Jan",
        previousPeriodDateRange = "7-13 Jan",
        chartData = ViewsStatsChartData(
            currentPeriod = currentPeriodLabels.zip(SAMPLE_CURRENT_PERIOD_DATA) { label, views ->
                ChartDataPoint(label, views)
            },
            previousPeriod = previousPeriodLabels.zip(SAMPLE_PREVIOUS_PERIOD_DATA) { label, views ->
                ChartDataPoint(label, views)
            }
        ),
        periodAverage = SAMPLE_PERIOD_AVERAGE,
        bottomStats = listOf(
            StatItem("Views", SAMPLE_CURRENT_VIEWS, StatChange.Negative(SAMPLE_VIEWS_PERCENTAGE)),
            StatItem("Visitors", SAMPLE_VISITORS, StatChange.Negative(SAMPLE_VISITORS_PERCENTAGE)),
            StatItem("Likes", 0, StatChange.NoChange),
            StatItem("Comments", 0, StatChange.NoChange),
            StatItem("Posts", SAMPLE_POSTS, StatChange.Positive(SAMPLE_POSTS_PERCENTAGE))
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun ViewsStatsCardLoadedPreview() {
    AppThemeM3 {
        ViewsStatsCard(
            uiState = sampleLoadedState(),
            onChartTypeChanged = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ViewsStatsCardErrorPreview() {
    AppThemeM3 {
        ViewsStatsCard(
            uiState = ViewsStatsCardUiState.Error(
                message = stringResource(R.string.stats_todays_stats_failed_to_load)
            ),
            onChartTypeChanged = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ViewsStatsCardLoadedDarkPreview() {
    AppThemeM3 {
        ViewsStatsCard(
            uiState = sampleLoadedState(),
            onChartTypeChanged = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}

/**
 * Custom marker value formatter that displays the date label and views count
 * for both current and previous period data points at the touched x-coordinate.
 */
private class ChartMarkerValueFormatter(
    private val currentPeriodData: List<ChartDataPoint>,
    private val previousPeriodData: List<ChartDataPoint>
) : DefaultCartesianMarker.ValueFormatter {
    override fun format(
        context: CartesianDrawingContext,
        targets: List<CartesianMarker.Target>
    ): CharSequence {
        val target = targets.firstOrNull() ?: return ""
        val x = target.x.toInt()
        return formatBothPeriods(x)
    }

    private fun formatBothPeriods(x: Int): String {
        val hasCurrent = x in currentPeriodData.indices
        val hasPrevious = x in previousPeriodData.indices

        if (!hasCurrent && !hasPrevious) return ""

        return buildString {
            // Show current period with date and value
            if (hasCurrent) {
                val current = currentPeriodData[x]
                append("● ${current.label}: ${formatStatValue(current.views)}")
            }
            // Show previous period with date and value
            if (hasPrevious) {
                if (hasCurrent) append("\n")
                val previous = previousPeriodData[x]
                append("○ ${previous.label}: ${formatStatValue(previous.views)}")
            }
        }
    }
}
