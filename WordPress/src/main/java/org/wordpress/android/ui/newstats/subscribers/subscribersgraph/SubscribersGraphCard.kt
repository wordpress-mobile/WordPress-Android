package org.wordpress.android.ui.newstats.subscribers.subscribersgraph

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
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
import org.wordpress.android.ui.newstats.components.CardPosition
import org.wordpress.android.ui.newstats.components.StatsCardContainer
import org.wordpress.android.ui.newstats.components.StatsCardErrorContent
import org.wordpress.android.ui.newstats.components.StatsCardHeader
import org.wordpress.android.ui.newstats.util.ShimmerBox
import org.wordpress.android.ui.newstats.util.formatStatValue

private val CardPadding = 16.dp
private val ChartHeight = 160.dp

@Suppress("LongParameterList")
@Composable
fun SubscribersGraphCard(
    uiState: SubscribersGraphUiState,
    selectedTab: SubscribersGraphTab,
    onTabSelected: (SubscribersGraphTab) -> Unit,
    onRetry: () -> Unit,
    onRemoveCard: () -> Unit,
    modifier: Modifier = Modifier,
    cardPosition: CardPosition? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveToTop: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onMoveToBottom: (() -> Unit)? = null
) {
    StatsCardContainer(modifier = modifier) {
        when (uiState) {
            is SubscribersGraphUiState.Loading ->
                LoadingContent(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    onRemoveCard = onRemoveCard,
                    cardPosition = cardPosition,
                    onMoveUp = onMoveUp,
                    onMoveToTop = onMoveToTop,
                    onMoveDown = onMoveDown,
                    onMoveToBottom = onMoveToBottom
                )
            is SubscribersGraphUiState.Loaded ->
                LoadedContent(
                    state = uiState,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    onRemoveCard = onRemoveCard,
                    cardPosition = cardPosition,
                    onMoveUp = onMoveUp,
                    onMoveToTop = onMoveToTop,
                    onMoveDown = onMoveDown,
                    onMoveToBottom = onMoveToBottom
                )
            is SubscribersGraphUiState.Error ->
                StatsCardErrorContent(
                    titleResId =
                        R.string.stats_subscribers_graph,
                    errorMessageResId =
                        R.string.stats_error_api,
                    onRetry = onRetry,
                    onRemoveCard = onRemoveCard,
                    cardPosition = cardPosition,
                    onMoveUp = onMoveUp,
                    onMoveToTop = onMoveToTop,
                    onMoveDown = onMoveDown,
                    onMoveToBottom = onMoveToBottom
                )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList")
@Composable
private fun LoadingContent(
    selectedTab: SubscribersGraphTab,
    onTabSelected: (SubscribersGraphTab) -> Unit,
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
        StatsCardHeader(
            titleResId =
                R.string.stats_subscribers_graph,
            onRemoveCard = onRemoveCard,
            cardPosition = cardPosition,
            onMoveUp = onMoveUp,
            onMoveToTop = onMoveToTop,
            onMoveDown = onMoveDown,
            onMoveToBottom = onMoveToBottom
        )
        Spacer(modifier = Modifier.height(12.dp))
        PeriodTabs(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
        )
        Spacer(modifier = Modifier.height(16.dp))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChartHeight)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList")
@Composable
private fun LoadedContent(
    state: SubscribersGraphUiState.Loaded,
    selectedTab: SubscribersGraphTab,
    onTabSelected: (SubscribersGraphTab) -> Unit,
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
        StatsCardHeader(
            titleResId =
                R.string.stats_subscribers_graph,
            onRemoveCard = onRemoveCard,
            cardPosition = cardPosition,
            onMoveUp = onMoveUp,
            onMoveToTop = onMoveToTop,
            onMoveDown = onMoveDown,
            onMoveToBottom = onMoveToBottom
        )
        Spacer(modifier = Modifier.height(12.dp))
        PeriodTabs(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
        )
        Spacer(modifier = Modifier.height(16.dp))
        SubscribersChart(
            dataPoints = state.dataPoints
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodTabs(
    selectedTab: SubscribersGraphTab,
    onTabSelected: (SubscribersGraphTab) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        SubscribersGraphTab.entries.forEachIndexed {
                index, tab ->
            SegmentedButton(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count =
                        SubscribersGraphTab.entries.size
                ),
                label = {
                    Text(
                        text = stringResource(
                            tab.labelResId
                        ),
                        fontSize = 12.sp
                    )
                }
            )
        }
    }
}

@Composable
private fun SubscribersChart(
    dataPoints: List<GraphDataPoint>
) {
    if (dataPoints.isEmpty()) return

    val modelProducer = remember {
        CartesianChartModelProducer()
    }

    LaunchedEffect(dataPoints) {
        modelProducer.runTransaction {
            lineSeries {
                series(
                    dataPoints.map { it.count }
                )
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    val dateLabels = dataPoints.map { it.label }
    val bottomAxisValueFormatter =
        CartesianValueFormatter { _, value, _ ->
            val index = value.toInt()
            if (index in dateLabels.indices) {
                dateLabels[index]
            } else {
                ""
            }
        }

    val markerValueFormatter = remember(dataPoints) {
        SubscribersMarkerValueFormatter(dataPoints)
    }

    val marker = rememberDefaultCartesianMarker(
        label = rememberTextComponent(
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp
            ),
            lineCount = 2,
            padding = Insets(
                horizontal = 12.dp,
                vertical = 8.dp
            ),
            background = rememberShapeComponent(
                fill = Fill(
                    MaterialTheme.colorScheme
                        .surfaceContainer
                ),
                shape = RoundedCornerShape(25)
            )
        ),
        guideline = LineComponent(
            fill = Fill(
                MaterialTheme.colorScheme.outline
                    .copy(alpha = 0.5f)
            ),
            thickness = 1.dp
        ),
        valueFormatter = markerValueFormatter
    )

    val areaGradient =
        Brush.verticalGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.3f),
                primaryColor.copy(alpha = 0f)
            )
        )

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider =
                    LineCartesianLayer.LineProvider
                        .series(
                            LineCartesianLayer.Line(
                                fill = LineCartesianLayer
                                    .LineFill.single(
                                        Fill(primaryColor)
                                    ),
                                areaFill =
                                    LineCartesianLayer
                                        .AreaFill.single(
                                            Fill(
                                                areaGradient
                                            )
                                        ),
                                interpolator =
                                    LineCartesianLayer
                                        .Interpolator
                                        .cubic()
                            )
                        )
            ),
            startAxis = VerticalAxis.rememberStart(
                line = null
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter =
                    bottomAxisValueFormatter
            ),
            marker = marker
        ),
        modelProducer = modelProducer,
        scrollState = rememberVicoScrollState(
            scrollEnabled = false
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(ChartHeight)
    )
}

private class SubscribersMarkerValueFormatter(
    private val dataPoints: List<GraphDataPoint>
) : DefaultCartesianMarker.ValueFormatter {
    @Suppress("ReturnCount")
    override fun format(
        context: CartesianDrawingContext,
        targets: List<CartesianMarker.Target>
    ): CharSequence {
        val target =
            targets.firstOrNull() ?: return ""
        val x = target.x.toInt()
        if (x !in dataPoints.indices) return ""
        val point = dataPoints[x]
        return "${point.label}\n" +
            formatStatValue(point.count)
    }
}
