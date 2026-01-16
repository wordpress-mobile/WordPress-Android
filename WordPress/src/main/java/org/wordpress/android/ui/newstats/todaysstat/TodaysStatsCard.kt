package org.wordpress.android.ui.newstats.todaysstat

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CardCornerRadius = 16.dp
private val CardPadding = 16.dp
private val ChartHeight = 80.dp
private val MetricIconSize = 16.dp
private val MetricSpacing = 4.dp

@Composable
fun TodaysStatsCard(
    uiState: TodaysStatsCardUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardCornerRadius)),
        shape = RoundedCornerShape(CardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CardPadding)
    ) {
        CardHeader()
        Spacer(modifier = Modifier.height(12.dp))
        // Placeholder for chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChartHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Placeholder for metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                )
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
        CardHeader()
        Spacer(modifier = Modifier.height(12.dp))
        StatsChart(chartData = state.chartData)
        Spacer(modifier = Modifier.height(16.dp))
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
            .padding(CardPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CardHeader()
        Spacer(modifier = Modifier.height(24.dp))
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

@Composable
private fun CardHeader() {
    val dateFormat = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
    val formattedDate = remember { dateFormat.format(Date()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.stats_insights_today),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
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

    LaunchedEffect(chartData) {
        if (chartData.currentPeriod.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    series(chartData.currentPeriod.map { it.views.toInt() })
                }
                if (chartData.previousPeriod.isNotEmpty()) {
                    lineSeries {
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

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.Line(
                        fill = LineCartesianLayer.LineFill.single(fill(primaryColor)),
                        areaFill = LineCartesianLayer.AreaFill.single(
                            fill(primaryColor.copy(alpha = 0.2f))
                        )
                    ),
                    LineCartesianLayer.Line(
                        fill = LineCartesianLayer.LineFill.single(fill(secondaryColor))
                    )
                )
            )
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(ChartHeight)
    )
}

@Composable
private fun MetricsRow(
    views: Int,
    visitors: Int,
    likes: Int,
    comments: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MetricItem(
            icon = Icons.Default.Visibility,
            value = formatStatValue(views),
            label = stringResource(R.string.stats_views),
            isPrimary = true
        )
        MetricItem(
            icon = Icons.Default.Person,
            value = formatStatValue(visitors),
            label = stringResource(R.string.stats_visitors)
        )
        MetricItem(
            icon = Icons.Default.FavoriteBorder,
            value = formatStatValue(likes),
            label = stringResource(R.string.stats_likes)
        )
        MetricItem(
            icon = Icons.Default.ChatBubbleOutline,
            value = formatStatValue(comments),
            label = stringResource(R.string.stats_comments)
        )
    }
}

@Composable
private fun MetricItem(
    icon: ImageVector,
    value: String,
    label: String,
    isPrimary: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MetricSpacing)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(MetricIconSize),
                tint = if (isPrimary) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = value,
                style = if (isPrimary) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontWeight = FontWeight.Bold,
                color = if (isPrimary) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatStatValue(value: Int): String {
    return when {
        value >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM", value / 1_000_000.0)
        value >= 1_000 -> String.format(Locale.getDefault(), "%.1fK", value / 1_000.0)
        else -> value.toString()
    }
}

@Preview(showBackground = true)
@Composable
private fun TodaysStatsCardLoadingPreview() {
    AppThemeM3 {
        TodaysStatsCard(
            uiState = TodaysStatsCardUiState.Loading,
            modifier = Modifier.padding(16.dp)
        )
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
                        ViewsDataPoint("Mon", 100),
                        ViewsDataPoint("Tue", 150),
                        ViewsDataPoint("Wed", 120),
                        ViewsDataPoint("Thu", 200),
                        ViewsDataPoint("Fri", 180),
                        ViewsDataPoint("Sat", 250),
                        ViewsDataPoint("Sun", 220)
                    ),
                    previousPeriod = listOf(
                        ViewsDataPoint("Mon", 80),
                        ViewsDataPoint("Tue", 120),
                        ViewsDataPoint("Wed", 100),
                        ViewsDataPoint("Thu", 160),
                        ViewsDataPoint("Fri", 140),
                        ViewsDataPoint("Sat", 200),
                        ViewsDataPoint("Sun", 180)
                    )
                ),
                onCardClick = {}
            ),
            modifier = Modifier.padding(16.dp)
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
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
