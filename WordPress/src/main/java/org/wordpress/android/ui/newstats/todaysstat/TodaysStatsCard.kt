package org.wordpress.android.ui.newstats.todaysstat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.shader.verticalGradient
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CardCornerRadius = 10.dp
private val CardPadding = 16.dp
private val CardMargin = 16.dp
private val ChartHeight = 80.dp
private val MetricIconSize = 16.dp
private val MetricSpacing = 4.dp
private const val THOUSAND = 1_000
private const val MILLION = 1_000_000

@Composable
fun TodaysStatsCard(
    uiState: TodaysStatsCardUiState,
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
            is TodaysStatsCardUiState.Loading -> LoadingContent()
            is TodaysStatsCardUiState.Loaded -> LoadedContent(uiState)
            is TodaysStatsCardUiState.Error -> ErrorContent(uiState)
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
    val translateAnimation by transition.animateFloat(
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
        start = Offset(translateAnimation - 500f, 0f),
        end = Offset(translateAnimation, 0f)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CardPadding)
    ) {
        // Top section: Title + Chart placeholder
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Title and date placeholder
            Column(modifier = Modifier.weight(0.4f)) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(24.dp)
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
            // Right: Chart placeholder
            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .height(ChartHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerBrush)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Bottom section: Metrics placeholder
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            // Views placeholder (larger)
            Column {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
            Spacer(modifier = Modifier.width(24.dp))
            // Other metrics placeholder
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}

@Composable
private fun LoadedContent(state: TodaysStatsCardUiState.Loaded) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = state.onCardClick)
            .padding(CardPadding)
    ) {
        // Top section: Title/Date on left, Chart on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Left: Title and date
            TitleSection()
            Spacer(modifier = Modifier.width(16.dp))
            // Right: Chart
            Box(modifier = Modifier.weight(1f)) {
                StatsChart(chartData = state.chartData)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Bottom section: Metrics
        MetricsRow(
            views = state.views,
            visitors = state.visitors,
            likes = state.likes,
            comments = state.comments
        )
    }
}

@Composable
private fun ErrorContent(state: TodaysStatsCardUiState.Error) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CardPadding)
    ) {
        // Top section: Title/Date on left, Empty chart placeholder on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Left: Title and date
            TitleSection()
            Spacer(modifier = Modifier.width(16.dp))
            // Right: Empty chart placeholder
            Box(
                modifier = Modifier
                    .weight(1f)
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
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Error message and retry button centered
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

@Composable
private fun TitleSection() {
    val dateFormat = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
    val formattedDate = remember { dateFormat.format(Date()) }

    Column {
        Text(
            text = stringResource(R.string.stats_insights_today_stats),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatsChart(chartData: ChartData) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val hasPreviousPeriod = chartData.previousPeriod.isNotEmpty()

    LaunchedEffect(chartData) {
        if (chartData.currentPeriod.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    // Today's data (solid line)
                    series(chartData.currentPeriod.map { it.views.toInt() })
                    // Yesterday's data (dashed line) - only if available
                    if (hasPreviousPeriod) {
                        series(chartData.previousPeriod.map { it.views.toInt() })
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

    // Create gradient shader for area fill (primary color fading to transparent)
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
                    // Today's line - solid with gradient area fill, curved
                    LineCartesianLayer.Line(
                        fill = LineCartesianLayer.LineFill.single(fill(primaryColor)),
                        areaFill = LineCartesianLayer.AreaFill.single(fill(areaGradient)),
                        pointConnector = LineCartesianLayer.PointConnector.cubic()
                    ),
                    // Yesterday's line - dashed, no area fill, curved
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
private fun MetricsRow(
    views: Long,
    visitors: Long,
    likes: Long,
    comments: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        // Views - prominent on the left
        PrimaryMetricItem(
            value = formatStatValue(views),
            label = stringResource(R.string.stats_views)
        )
        Spacer(modifier = Modifier.weight(1f))
        // Secondary metrics on the right
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SecondaryMetricItem(
                icon = Icons.Default.Person,
                value = formatStatValue(visitors)
            )
            SecondaryMetricItem(
                icon = Icons.Default.FavoriteBorder,
                value = formatStatValue(likes)
            )
            SecondaryMetricItem(
                icon = Icons.Default.ChatBubbleOutline,
                value = formatStatValue(comments)
            )
        }
    }
}

@Composable
private fun PrimaryMetricItem(
    value: String,
    label: String
) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SecondaryMetricItem(
    icon: ImageVector,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MetricSpacing)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(MetricIconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatStatValue(value: Long): String {
    return when {
        value >= MILLION -> String.format(Locale.getDefault(), "%.1fM", value / MILLION.toDouble())
        value >= THOUSAND -> String.format(Locale.getDefault(), "%.1fK", value / THOUSAND.toDouble())
        else -> value.toString()
    }
}

@Preview(showBackground = true)
@Composable
private fun TodaysStatsCardLoadingPreview() {
    AppThemeM3 {
        TodaysStatsCard(uiState = TodaysStatsCardUiState.Loading)
    }
}

@Preview(showBackground = true)
@Composable
private fun TodaysStatsCardLoadedPreview() {
    AppThemeM3 {
        TodaysStatsCard(
            uiState = TodaysStatsCardUiState.Loaded(
                views = 1234,
                visitors = 567,
                likes = 89,
                comments = 12,
                chartData = ChartData(
                    currentPeriod = listOf(
                        ViewsDataPoint("12am", 100),
                        ViewsDataPoint("4am", 50),
                        ViewsDataPoint("8am", 150),
                        ViewsDataPoint("12pm", 200),
                        ViewsDataPoint("4pm", 180),
                        ViewsDataPoint("8pm", 250)
                    ),
                    previousPeriod = listOf(
                        ViewsDataPoint("12am", 80),
                        ViewsDataPoint("4am", 40),
                        ViewsDataPoint("8am", 120),
                        ViewsDataPoint("12pm", 160),
                        ViewsDataPoint("4pm", 140),
                        ViewsDataPoint("8pm", 200)
                    )
                ),
                onCardClick = {}
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TodaysStatsCardErrorPreview() {
    AppThemeM3 {
        TodaysStatsCard(
            uiState = TodaysStatsCardUiState.Error(
                message = "Failed to load stats",
                onRetry = {}
            )
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TodaysStatsCardLoadedDarkPreview() {
    AppThemeM3 {
        TodaysStatsCard(
            uiState = TodaysStatsCardUiState.Loaded(
                views = 1234,
                visitors = 567,
                likes = 89,
                comments = 12,
                chartData = ChartData(
                    currentPeriod = listOf(
                        ViewsDataPoint("12am", 100),
                        ViewsDataPoint("4am", 50),
                        ViewsDataPoint("8am", 150),
                        ViewsDataPoint("12pm", 200),
                        ViewsDataPoint("4pm", 180),
                        ViewsDataPoint("8pm", 250)
                    ),
                    previousPeriod = listOf(
                        ViewsDataPoint("12am", 80),
                        ViewsDataPoint("4am", 40),
                        ViewsDataPoint("8am", 120),
                        ViewsDataPoint("12pm", 160),
                        ViewsDataPoint("4pm", 140),
                        ViewsDataPoint("8pm", 200)
                    )
                ),
                onCardClick = {}
            )
        )
    }
}
