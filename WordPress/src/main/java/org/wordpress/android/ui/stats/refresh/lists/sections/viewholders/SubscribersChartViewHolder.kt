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
import com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.LineChartMarkerView
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.SubscribersChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.SubscribersChartItem.Line
import org.wordpress.android.ui.stats.refresh.utils.LargeValueFormatter
import org.wordpress.android.ui.stats.refresh.utils.LineChartAccessibilityHelper
import org.wordpress.android.ui.stats.refresh.utils.LineChartAccessibilityHelper.LineChartAccessibilityEvent
import org.wordpress.android.ui.stats.refresh.utils.SubscribersChartLabelFormatter

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
            if (hasData(item.entries)) {
                chart.post {
                    val accessibilityEvent = object : LineChartAccessibilityEvent {
                        override fun onHighlight(entry: Entry, index: Int) {
                            drawChartMarker(Highlight(entry.x, entry.y, 0))
                            val value = entry.data as? String
                            value?.let {
                                item.onLineSelected?.invoke(it)
                            }
                        }
                    }

                    val cutContentDescriptions = takeEntriesWithinGraphWidth(item.entryContentDescriptions)
                    accessibilityHelper = LineChartAccessibilityHelper(
                        chart,
                        contentDescriptions = cutContentDescriptions,
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
        val data = if (hasData(item.entries)) {
            val mappedEntries = item.entries.mapIndexed { index, pair -> toLineEntry(pair, index) }
            LineDataSet(mappedEntries, null)
        } else {
            buildEmptyDataSet(item.entries.size)
        }
        item.onLineChartDrawn?.invoke(data.entryCount)

        return listOf(data)
    }

    private fun hasData(entries: List<Line>) = entries.isNotEmpty() && entries.any { it.value > 0 }

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
                    override fun onNothingSelected() {
                        item.onLineSelected?.invoke(item.selectedItemPeriod)
                    }

                    override fun onValueSelected(e: Entry, h: Highlight) {
                        drawChartMarker(h)
                        item.onLineSelected?.invoke(e.data as? String)
                    }
                })
            } else {
                setOnChartValueSelectedListener(null)
            }
            isHighlightPerTapEnabled = isClickable
        }
    }

    private fun configureYAxis(item: SubscribersChartItem) {
        val minYValue = 6f
        val maxYValue = item.entries.maxByOrNull { it.value }?.value ?: 7

        chart.axisLeft.apply {
            valueFormatter = LargeValueFormatter()
            setDrawGridLines(true)
            setDrawTopYLabelEntry(true)
            setDrawZeroLine(true)
            setDrawAxisLine(false)
            granularity = 1F
            axisMinimum = 0F
            axisMaximum = if (maxYValue < minYValue) {
                minYValue
            } else {
                roundUp(maxYValue.toFloat())
            }
            setLabelCount(5, true)
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
                axisLineWidth = 4.0F

                val tickWidth = 4.0F
                val contentWidthMinusTicks = chart.contentRect.width() - (tickWidth * 3f)
                enableAxisLineDashedLine(tickWidth, contentWidthMinusTicks / 29f, 0f)
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
            val markerView = LineChartMarkerView(chart.context, 0)
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

            mode = CUBIC_BEZIER
            cubicIntensity = 0.2f
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

    private fun <T> takeEntriesWithinGraphWidth(entries: List<T>) = if (8 < entries.size) {
        entries.subList(entries.size - 8, entries.size)
    } else {
        entries
    }

    private fun LineChart.resetChart() {
        fitScreen()
        data?.clearValues()
        xAxis.valueFormatter = null
        notifyDataSetChanged()
        clear()
        invalidate()
    }

    private fun toLineEntry(line: Line, index: Int) = Entry(index.toFloat(), line.value.toFloat(), line.id)

    private fun roundUp(input: Float): Float {
        return if (input > 100) {
            roundUp(input / 10) * 10
        } else {
            for (i in 1..25) {
                val limit = 4 * i
                if (input < limit) {
                    return limit.toFloat()
                }
            }
            100F
        }
    }
}
