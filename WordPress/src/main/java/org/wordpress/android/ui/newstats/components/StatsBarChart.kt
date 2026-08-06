package org.wordpress.android.ui.newstats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Zero-value entries still get a sliver of a bar so the series reads as continuous.
private const val MIN_BAR_FRACTION = 0.02f

/**
 * A compact bar chart of a single series, scaled against its own largest value. Renders nothing
 * when every value is zero.
 */
@Composable
fun StatsBarChart(
    values: List<Long>,
    height: Dp,
    modifier: Modifier = Modifier,
    barSpacing: Dp = 2.dp
) {
    val maxValue = values.maxOrNull() ?: 0L
    if (maxValue <= 0L) return

    val barColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.spacedBy(barSpacing),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEach { value ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                val fraction = (
                    value.toFloat() / maxValue.toFloat()
                    ).coerceIn(MIN_BAR_FRACTION, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction)
                        .clip(
                            RoundedCornerShape(
                                topStart = 2.dp,
                                topEnd = 2.dp
                            )
                        )
                        .background(barColor)
                )
            }
        }
    }
}
