package org.wordpress.android.ui.newstats.viewsstats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.ModeEditOutline
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import org.wordpress.android.ui.newstats.StatsColors
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.newstats.components.CardPosition
import org.wordpress.android.ui.newstats.components.StatsCardMenu
import org.wordpress.android.ui.newstats.util.formatStatValue
import org.wordpress.android.ui.newstats.util.ShimmerBox
import org.wordpress.android.ui.newstats.util.rememberShimmerBrush
import java.util.Locale
import kotlin.math.abs

private val CardCornerRadius = 10.dp
private val CardPadding = 16.dp
private val CardMargin = 16.dp
private val ChartHeight = 180.dp
private val StatItemWidth = 100.dp
private val BadgeCornerRadius = 4.dp

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
    onBarTapped: (Int) -> Unit,
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
            is ViewsStatsCardUiState.Content -> ContentCard(
                uiState, onChartTypeChanged, onBarTapped, onRetry, onRemoveCard,
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CardPadding)
    ) {
        // Header shimmer
        ShimmerBox(
            modifier = Modifier
                .width(60.dp)
                .height(24.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        ChartRegionPlaceholder()
        Spacer(modifier = Modifier.height(16.dp))
        BottomStatsPlaceholder()
    }
}

/**
 * Placeholder for the chart region (totals row + chart area) while the chart calls are in flight.
 */
@Composable
private fun ChartRegionPlaceholder() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column {
                ShimmerBox(modifier = Modifier.width(80.dp).height(32.dp))
                Spacer(modifier = Modifier.height(4.dp))
                ShimmerBox(modifier = Modifier.width(100.dp).height(16.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                ShimmerBox(modifier = Modifier.width(80.dp).height(16.dp))
                Spacer(modifier = Modifier.height(4.dp))
                ShimmerBox(modifier = Modifier.width(80.dp).height(16.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChartHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(rememberShimmerBrush())
        )
    }
}

/** Placeholder for the bottom-stats row while the dedicated bottom call is in flight. */
@Composable
private fun BottomStatsPlaceholder() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(4) {
            ShimmerBox(
                modifier = Modifier
                    .width(70.dp)
                    .height(60.dp)
            )
        }
    }
}

@Composable
private fun ContentCard(
    state: ViewsStatsCardUiState.Content,
    onChartTypeChanged: (ChartType) -> Unit,
    onBarTapped: (Int) -> Unit,
    onRetry: () -> Unit,
    onRemoveCard: () -> Unit,
    cardPosition: CardPosition?,
    onMoveUp: (() -> Unit)?,
    onMoveToTop: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onMoveToBottom: (() -> Unit)?
) {
    val chart = state.chart
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding)
                .alpha(if (state.isLoadingNewPeriod) 0.5f else 1f)
        ) {
            // Title + menu are always shown so the card controls stay reachable in every state.
            // Chart-type options only make sense once the chart has loaded.
            CardTitleRow(
                chartTypeMenu = if (chart is ChartUiState.Loaded) {
                    { ChartTypeMenuItems(chart.chartType, onChartTypeChanged) }
                } else {
                    null
                },
                onRemoveCard = onRemoveCard,
                cardPosition = cardPosition,
                onMoveUp = onMoveUp,
                onMoveToTop = onMoveToTop,
                onMoveDown = onMoveDown,
                onMoveToBottom = onMoveToBottom
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Chart region — loads independently of the bottom row
            when (chart) {
                is ChartUiState.Loading -> ChartRegionPlaceholder()
                is ChartUiState.Loaded -> {
                    HeaderStats(chart)
                    Spacer(modifier = Modifier.height(8.dp))
                    ViewsStatsChart(
                        chartData = chart.chartData,
                        periodAverage = chart.periodAverage,
                        chartType = chart.chartType,
                        onBarTapped = onBarTapped
                    )
                }
                is ChartUiState.Error -> ChartErrorBox(onRetry = onRetry)
            }
            // Bottom Stats Row — loads independently; hidden when its dedicated call failed
            when (val bottom = state.bottomStats) {
                is BottomStatsUiState.Loading -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    BottomStatsPlaceholder()
                }
                is BottomStatsUiState.Loaded -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    BottomStatsRow(stats = bottom.stats)
                }
                is BottomStatsUiState.Hidden -> Unit
            }
        }
        if (state.isLoadingNewPeriod) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CardTitleRow(
    chartTypeMenu: @Composable (() -> Unit)?,
    onRemoveCard: () -> Unit,
    cardPosition: CardPosition?,
    onMoveUp: (() -> Unit)?,
    onMoveToTop: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onMoveToBottom: (() -> Unit)?
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
            onMoveToBottom = onMoveToBottom,
            additionalContent = chartTypeMenu
        )
    }
}

@Composable
private fun HeaderStats(chart: ChartUiState.Loaded) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Left: Current and previous period totals with difference
        Column {
            Row {
                Text(
                    text = formatStatValue(chart.currentPeriodViews),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.alignByBaseline()
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatStatValue(chart.previousPeriodViews),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline()
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            DifferenceRow(
                difference = chart.viewsDifference,
                percentageChange = chart.viewsPercentageChange
            )
        }
        // Right: Date ranges with colored dots and average
        Column(horizontalAlignment = Alignment.End) {
            DateRangeWithDot(
                dateRange = chart.currentPeriodDateRange,
                dotColor = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            DateRangeWithDot(
                dateRange = chart.previousPeriodDateRange,
                dotColor = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(4.dp))
            AverageRow(average = chart.periodAverage)
        }
    }
}

/**
 * Compact error shown in place of the header + chart when the chart calls fail. The bottom-stats
 * row is rendered separately, so it stays visible even while this is shown.
 */
@Composable
private fun ChartErrorBox(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ChartHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.stats_error_api),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.retry))
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
                imageVector = Icons.AutoMirrored.Outlined.ShowChart,
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
                imageVector = Icons.Outlined.BarChart,
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
        difference > 0 -> StatsColors.ChangeBadgePositive
        else -> MaterialTheme.colorScheme.outline
    }
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.ROOT

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
            text = String.format(locale, "%.1f%%", abs(percentageChange)),
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

@Composable
private fun DateRangeWithDot(dateRange: String, dotColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = dateRange,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
    }
}

@Composable
private fun AverageRow(average: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.stats_weekly_average, formatStatValue(average)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
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
    chartType: ChartType,
    onBarTapped: (Int) -> Unit = {}
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
                    lineModel {
                        series(chartData.currentPeriod.map { it.views })
                        if (hasPreviousPeriod) {
                            series(chartData.previousPeriod.map { it.views })
                        }
                    }
                }
                ChartType.BAR -> modelProducer.runTransaction {
                    columnModel {
                        if (hasPreviousPeriod) {
                            // Series 1: current value when no
                            // delta (rounded top)
                            series(
                                chartData.currentPeriod.zip(
                                    chartData.previousPeriod
                                ) { current, previous ->
                                    if (previous.views <= current.views)
                                        current.views
                                    else 0L
                                }
                            )
                            // Series 2: current value when there
                            // is a delta (flat top)
                            series(
                                chartData.currentPeriod.zip(
                                    chartData.previousPeriod
                                ) { current, previous ->
                                    if (previous.views > current.views)
                                        current.views
                                    else 0L
                                }
                            )
                            // Series 3: delta (rounded top)
                            series(
                                chartData.currentPeriod.zip(
                                    chartData.previousPeriod
                                ) { current, previous ->
                                    maxOf(
                                        0L,
                                        previous.views - current.views
                                    )
                                }
                            )
                        } else {
                            series(
                                chartData.currentPeriod.map {
                                    it.views
                                }
                            )
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

    // X-axis labels from both series so the formatter covers the
    // full range even when the previous period has more data points
    val currentLabels = chartData.currentPeriod.map { it.label }
    val previousLabels = chartData.previousPeriod.map { it.label }
    val dateLabels = if (previousLabels.size > currentLabels.size) {
        currentLabels + previousLabels.drop(currentLabels.size)
    } else {
        currentLabels
    }
    val bottomAxisValueFormatter = CartesianValueFormatter { _, value, _ ->
        dateLabels.getOrElse(value.toInt()) { value.toInt().toString() }
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
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp
            ),
            lineCount = 3,
            padding = Insets(horizontal = 12.dp, vertical = 8.dp),
            background = rememberShapeComponent(
                fill = Fill(MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(25)
            )
        ),
        labelPosition = DefaultCartesianMarker.LabelPosition.AroundPoint,
        guideline = LineComponent(
            fill = Fill(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            thickness = 1.dp
        ),
        valueFormatter = markerValueFormatter
    )

    // Horizontal line for period average
    val averageLine = HorizontalLine(
        y = { periodAverage.toDouble() },
        line = LineComponent(
            fill = Fill(MaterialTheme.colorScheme.outline),
            thickness = 1.dp
        )
    )

    when (chartType) {
        ChartType.LINE -> {
            val areaGradient = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.3f),
                    primaryColor.copy(alpha = 0f)
                )
            )

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        lineProvider = LineCartesianLayer.LineProvider.series(
                            LineCartesianLayer.Line(
                                fill = LineCartesianLayer.LineFill.single(Fill(primaryColor)),
                                areaFill = LineCartesianLayer.AreaFill.single(Fill(areaGradient)),
                                interpolator = LineCartesianLayer.Interpolator.Sharp
                            ),
                            LineCartesianLayer.Line(
                                fill = LineCartesianLayer.LineFill.single(Fill(secondaryColor)),
                                stroke = LineCartesianLayer.LineStroke.Dashed(),
                                interpolator = LineCartesianLayer.Interpolator.Sharp
                            )
                        )
                    ),
                    startAxis = VerticalAxis.rememberStart(line = null),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = bottomAxisValueFormatter
                    ),
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
            // The marker shows on pointer-down and hides on pointer-up, so its visibility
            // callbacks cannot tell a tap from the start of a scroll. Track which bar the
            // pointer is over here, and drill down only once detectTapGestures confirms the
            // gesture was a tap -- an enclosing scroll consumes the drag, which cancels it.
            // Hit-testing is horizontal only, so a tap anywhere in this box selects the
            // nearest bar, matching the marker's existing behavior.
            var pressedIndex by remember { mutableIntStateOf(-1) }
            val barTapListener = remember {
                object : CartesianMarkerVisibilityListener {
                    override fun onShown(
                        marker: CartesianMarker,
                        targets: List<CartesianMarker.Target>
                    ) {
                        pressedIndex = targets.firstOrNull()?.x?.toInt() ?: -1
                    }

                    override fun onUpdated(
                        marker: CartesianMarker,
                        targets: List<CartesianMarker.Target>
                    ) {
                        pressedIndex = targets.firstOrNull()?.x?.toInt() ?: -1
                    }

                    override fun onHidden(marker: CartesianMarker) {
                        pressedIndex = -1
                    }
                }
            }
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(
                        columnProvider = ColumnCartesianLayer
                            .ColumnProvider.series(
                                LineComponent(
                                    fill = Fill(primaryColor),
                                    thickness = 16.dp,
                                    shape = RoundedCornerShape(
                                        topStartPercent = 20,
                                        topEndPercent = 20,
                                    )
                                ),
                                LineComponent(
                                    fill = Fill(primaryColor),
                                    thickness = 16.dp,
                                ),
                                LineComponent(
                                    fill = Fill(secondaryColor),
                                    thickness = 16.dp,
                                    shape = RoundedCornerShape(
                                        topStartPercent = 20,
                                        topEndPercent = 20,
                                    )
                                )
                            ),
                        mergeMode = {
                            ColumnCartesianLayer.MergeMode.Stacked
                        }
                    ),
                    startAxis = VerticalAxis.rememberStart(
                        line = null
                    ),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = bottomAxisValueFormatter
                    ),
                    marker = marker,
                    markerVisibilityListener = barTapListener,
                    decorations = listOf(averageLine)
                ),
                modelProducer = modelProducer,
                scrollState = rememberVicoScrollState(
                    scrollEnabled = false
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ChartHeight)
                    .pointerInput(onBarTapped) {
                        detectTapGestures {
                            if (pressedIndex >= 0) onBarTapped(pressedIndex)
                        }
                    }
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
        stringResource(R.string.stats_views) -> Icons.Outlined.Visibility
        stringResource(R.string.stats_visitors) -> Icons.Default.PersonOutline
        stringResource(R.string.stats_likes) -> Icons.Default.FavoriteBorder
        stringResource(R.string.stats_comments) -> Icons.Default.ChatBubbleOutline
        stringResource(R.string.posts) -> Icons.Outlined.ModeEditOutline
        else -> Icons.Outlined.Visibility
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
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.ROOT
    val (text, backgroundColor, textColor) = when (change) {
        is StatChange.Positive -> Triple(
            "↗ ${String.format(locale, "%.1f%%", change.percentage)}",
            StatsColors.ChangeBadgePositive.copy(alpha = 0.15f),
            StatsColors.ChangeBadgePositive
        )
        is StatChange.Negative -> Triple(
            "↘ ${String.format(locale, "%.1f%%", change.percentage)}",
            StatsColors.ChangeBadgeNegative.copy(alpha = 0.15f),
            StatsColors.ChangeBadgeNegative
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
            style = MaterialTheme.typography.labelMedium,
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
            onBarTapped = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}

private fun sampleLoadedState(): ViewsStatsCardUiState.Content {
    val currentPeriodLabels = listOf("Jan 14", "Jan 15", "Jan 16", "Jan 17", "Jan 18", "Jan 19", "Jan 20")
    val previousPeriodLabels = listOf("Jan 7", "Jan 8", "Jan 9", "Jan 10", "Jan 11", "Jan 12", "Jan 13")

    return ViewsStatsCardUiState.Content(
        chart = ChartUiState.Loaded(
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
            periodAverage = SAMPLE_PERIOD_AVERAGE
        ),
        bottomStats = BottomStatsUiState.Loaded(
            listOf(
                StatItem("Views", SAMPLE_CURRENT_VIEWS, StatChange.Negative(SAMPLE_VIEWS_PERCENTAGE)),
                StatItem("Visitors", SAMPLE_VISITORS, StatChange.Negative(SAMPLE_VISITORS_PERCENTAGE)),
                StatItem("Likes", 0, StatChange.NoChange),
                StatItem("Comments", 0, StatChange.NoChange),
                StatItem("Posts", SAMPLE_POSTS, StatChange.Positive(SAMPLE_POSTS_PERCENTAGE))
            )
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
            onBarTapped = {},
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
                message = stringResource(R.string.stats_error_api)
            ),
            onChartTypeChanged = {},
            onBarTapped = {},
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
            onBarTapped = {},
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
