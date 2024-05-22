package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
import com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.LineDataSet.Mode.HORIZONTAL_BEZIER
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.SubscribersChartMarkerView
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.SubscribersChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.SubscribersChartItem.Line
import org.wordpress.android.ui.stats.refresh.utils.LargeValueFormatter
import org.wordpress.android.ui.stats.refresh.utils.LineChartAccessibilityHelper
import org.wordpress.android.ui.stats.refresh.utils.LineChartAccessibilityHelper.LineChartAccessibilityEvent
import org.wordpress.android.ui.stats.refresh.utils.SubscribersChartLabelFormatter
import org.wordpress.android.util.AppLog
import kotlin.math.max
import kotlin.math.min

@Suppress("MagicNumber")
class SubscribersChartViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
    parent,
    R.layout.stats_block_line_chart_item
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val chart = itemView.findViewById<LineChart>(R.id.line_chart)
    private lateinit var accessibilityHelper: LineChartAccessibilityHelper

    fun bind(item: SubscribersChartItem) {
        chart.setNoDataText("")

        coroutineScope.launch {
            delay(50)
            chart.draw(item)
            if (item.entries.isNotEmpty()) {
                chart.post {
                    val accessibilityEvent = object : LineChartAccessibilityEvent {
                        override fun onHighlight(entry: Entry, index: Int) {
                            drawChartMarker(Highlight(entry.x, entry.y, 0))
                            val value = entry.data as? String
                            value?.let { item.onLineSelected?.invoke() }
                        }
                    }

                    accessibilityHelper = LineChartAccessibilityHelper(
                        chart,
                        contentDescriptions = item.entryContentDescriptions,
                        accessibilityEvent = accessibilityEvent
                    )

                    ViewCompat.setAccessibilityDelegate(chart, accessibilityHelper)
                }
            }
        }
    }

    private fun LineChart.draw(item: SubscribersChartItem) {
        resetChart()

        data = LineData(getData(item))

        configureChartView(item)
        configureYAxis(item)
        configureXAxis(item)
        configureDataSets(data.dataSets)

        invalidate()
    }

    private fun getData(item: SubscribersChartItem): List<ILineDataSet> {
        val data = if (item.entries.isEmpty()) {
            buildEmptyDataSet(item.entries.size)
        } else {
            val mappedEntries = item.entries.mapIndexed { index, pair -> toLineEntry(pair, index) }
            LineDataSet(mappedEntries, null)
        }

        return listOf(data)
    }

    private fun configureChartView(item: SubscribersChartItem) {
        chart.apply {
            setPinchZoom(false)
            setScaleEnabled(false)
            legend.isEnabled = false
            setDrawBorders(false)
            extraLeftOffset = 16f
            axisRight.isEnabled = false

            isHighlightPerDragEnabled = false
            val description = Description()
            description.text = ""
            this.description = description

            animateX(1000, Easing.EaseInSine)

            val isClickable = item.onLineSelected != null
            if (isClickable) {
                setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                    override fun onNothingSelected() = Unit

                    override fun onValueSelected(e: Entry, h: Highlight) {
                        drawChartMarker(h)
                        item.onLineSelected?.invoke()
                    }
                })
            } else {
                setOnChartValueSelectedListener(null)
            }
            isHighlightPerTapEnabled = isClickable
        }
    }

    private fun configureYAxis(item: SubscribersChartItem) {
        val differentValueCount = item.entries.map { it.value }.distinct().size
        val hasChange = differentValueCount > 1
        val onlyZero = item.entries.all { it.value == 0 }
        val minYValue = if (hasChange) (item.entries.minByOrNull { it.value }?.value ?: 0) else 0
        val maxYValue = if (hasChange) {
            item.entries.maxByOrNull { it.value }?.value ?: 7
        } else {
            max(item.entries.last().value * 2, 1)
        }
        val labelCount = if (onlyZero) {
            2
        } else if (!hasChange) {
            3
        } else {
            min(5, differentValueCount)
        }

        chart.axisLeft.apply {
            valueFormatter = LargeValueFormatter()
            setDrawGridLines(true)
            setDrawTopYLabelEntry(true)
            setDrawZeroLine(true)
            setDrawAxisLine(false)
            granularity = 1F
            axisMinimum = minYValue.toFloat()
            axisMaximum = maxYValue.toFloat()
            setLabelCount(labelCount, true)
            textColor = ContextCompat.getColor(chart.context, R.color.neutral_30)
            gridColor = ContextCompat.getColor(chart.context, R.color.stats_bar_chart_gridline)
            textSize = 10f
            gridLineWidth = 1f
        }
    }

    private fun configureXAxis(item: SubscribersChartItem) {
        chart.xAxis.apply {
            granularity = 1f
            setDrawAxisLine(true)
            setDrawGridLines(false)

            if (chart.contentRect.width() > 0) {
                axisLineWidth = chart.resources.getDimensionPixelSize(R.dimen.stats_line_chart_tick_height) /
                    chart.resources.displayMetrics.density
                val tickWidth = chart.resources.getDimension(R.dimen.stats_line_chart_tick_width)
                val contentWidthMinusTicks = chart.contentRect.width() - tickWidth * 3
                enableAxisLineDashedLine(tickWidth, contentWidthMinusTicks / 2, 0f)
                axisLineColor = ContextCompat.getColor(chart.context, R.color.stats_bar_chart_gridline)
            }

            setDrawLabels(true)
            setLabelCount(3, true)
            setAvoidFirstLastClipping(true)
            position = BOTTOM
            valueFormatter = SubscribersChartLabelFormatter(item.entries)
            textColor = ContextCompat.getColor(chart.context, R.color.neutral_30)

            removeAllLimitLines()
        }
    }

    private fun drawChartMarker(h: Highlight) {
        if (chart.marker == null) {
            val markerView = SubscribersChartMarkerView(chart.context)
            markerView.chartView = chart
            chart.marker = markerView
        }
        chart.highlightValue(h)
    }

    private fun configureDataSets(dataSets: MutableList<ILineDataSet>) {
        (dataSets.first() as? LineDataSet)?.apply {
            axisDependency = LEFT

            lineWidth = 2f
            formLineWidth = 0f

            setDrawValues(false)
            setDrawCircles(false)

            highLightColor = ContextCompat.getColor(chart.context, R.color.gray_10)
            highlightLineWidth = 1F
            setDrawVerticalHighlightIndicator(true)
            setDrawHorizontalHighlightIndicator(false)
            enableDashedHighlightLine(4.4F, 1F, 0F)
            isHighlightEnabled = true

            mode = HORIZONTAL_BEZIER
            color = ContextCompat.getColor(chart.context, R.color.blue_50)

            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(
                chart.context,
                R.drawable.bg_rectangle_stats_line_chart_blue_gradient
            )?.apply { alpha = 26 }
        }
    }

    private fun buildEmptyDataSet(count: Int): LineDataSet {
        val emptyValues = (0 until count).map { index -> Entry(index.toFloat(), 0f, "empty") }
        val dataSet = LineDataSet(emptyValues, "Empty")

        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.isHighlightEnabled = false
        dataSet.fillAlpha = 80
        dataSet.setDrawHighlightIndicators(false)

        return dataSet
    }

    private fun LineChart.resetChart() {
        fitScreen()
        try {
            data?.clearValues()
        } catch (e: UnsupportedOperationException) {
            AppLog.e(AppLog.T.STATS, e)
        }
        xAxis.valueFormatter = null
        notifyDataSetChanged()
        clear()
        invalidate()
    }

    private fun toLineEntry(line: Line, index: Int) = Entry(index.toFloat(), line.value.toFloat(), line.id)
}
