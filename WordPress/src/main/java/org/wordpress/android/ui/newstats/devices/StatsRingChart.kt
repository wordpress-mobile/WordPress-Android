package org.wordpress.android.ui.newstats.devices

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3

private const val START_ANGLE = -90f
private const val FULL_CIRCLE = 360f
private const val GAP_DEGREES = 3f
private const val STROKE_WIDTH_RATIO = 0.15f

data class RingChartEntry(
    val label: String,
    val value: Double,
    val color: Color
)

@Composable
fun StatsRingChart(
    entries: List<RingChartEntry>,
    modifier: Modifier = Modifier
) {
    val entryDescriptionFormat = stringResource(
        R.string.stats_ring_chart_entry_description
    )
    val description = entries.joinToString(", ") {
        String.format(entryDescriptionFormat, it.label, it.value)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics {
                contentDescription = description
            }
    ) {
        val total = entries.sumOf { it.value }
        if (total <= 0) return@Canvas

        val strokeWidth = size.minDimension * STROKE_WIDTH_RATIO
        val radius = (size.minDimension - strokeWidth) / 2f
        val topLeft = Offset(
            (size.width - radius * 2) / 2f,
            (size.height - radius * 2) / 2f
        )
        val arcSize = Size(radius * 2, radius * 2)
        val totalGap = GAP_DEGREES * entries.size
        val availableDegrees = FULL_CIRCLE - totalGap

        var currentAngle = START_ANGLE
        entries.forEach { entry ->
            val sweep =
                (entry.value / total * availableDegrees).toFloat()
            drawArc(
                color = entry.color,
                startAngle = currentAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
            currentAngle += sweep + GAP_DEGREES
        }
    }
}

@Composable
fun ringChartColors(): List<Color> {
    return listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
    )
}

@Preview(showBackground = true)
@Composable
private fun StatsRingChartPreview() {
    AppThemeM3 {
        val colors = ringChartColors()
        StatsRingChart(
            entries = listOf(
                RingChartEntry("Desktop", 60.0, colors[0]),
                RingChartEntry("Mobile", 30.0, colors[1]),
                RingChartEntry("Tablet", 10.0, colors[2])
            )
        )
    }
}
