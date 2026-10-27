package org.wordpress.android.ui.newstats.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import org.wordpress.android.ui.newstats.StatsColors

private const val BAR_CORNER_PERCENT = 20
private val BarThickness = 16.dp

/**
 * A daily-views bar chart with value and date axes, highlighting one bar as the selected day.
 *
 * Vico colours a series rather than an individual bar, so the highlight is a second stacked series
 * that is zero everywhere except the selected index.
 */
@Composable
fun StatsDayViewsChart(
    values: List<Long>,
    selectedIndex: Int,
    height: Dp,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(values, selectedIndex) {
        modelProducer.runTransaction {
            columnModel {
                series(
                    values.mapIndexed { index, value ->
                        if (index == selectedIndex) 0L else value
                    }
                )
                series(
                    values.mapIndexed { index, value ->
                        if (index == selectedIndex) value else 0L
                    }
                )
            }
        }
    }

    val barColor = MaterialTheme.colorScheme.primary
    val highlightColor = StatsColors.ChartSelectedBar
    // These are remember() keys for the layer and the chart, so rebuilding them every
    // recomposition would rebuild the whole chart with them.
    val columnProvider = remember(barColor, highlightColor) {
        val barShape = RoundedCornerShape(
            topStartPercent = BAR_CORNER_PERCENT,
            topEndPercent = BAR_CORNER_PERCENT
        )
        ColumnCartesianLayer.ColumnProvider.series(
            LineComponent(
                fill = Fill(barColor),
                thickness = BarThickness,
                shape = barShape
            ),
            LineComponent(
                fill = Fill(highlightColor),
                thickness = BarThickness,
                shape = barShape
            )
        )
    }
    // Vico rejects blank labels, so the item placer -- not the formatter -- decides that only
    // the two ends are labelled.
    val bottomAxisValueFormatter =
        remember(startLabel, endLabel) {
            CartesianValueFormatter { _, x, _ ->
                if (x <= 0.0) startLabel else endLabel
            }
        }
    val endsOnlyItemPlacer = remember(values.size) {
        HorizontalAxis.ItemPlacer.aligned(
            spacing = { values.lastIndex.coerceAtLeast(1) }
        )
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = columnProvider,
                mergeMode = {
                    ColumnCartesianLayer.MergeMode.Stacked
                }
            ),
            startAxis = VerticalAxis.rememberStart(line = null),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = bottomAxisValueFormatter,
                itemPlacer = endsOnlyItemPlacer
            )
        ),
        modelProducer = modelProducer,
        scrollState = rememberVicoScrollState(
            scrollEnabled = false
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    )
}
