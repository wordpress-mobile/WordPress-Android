package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.graphics.DashPathEffect
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
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.LineChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.LineChartItem.Line
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.ViewsAndVisitorsMapper.SelectedType
import org.wordpress.android.ui.stats.refresh.utils.LargeValueFormatter
import org.wordpress.android.ui.stats.refresh.utils.LineChartAccessibilityHelper
import org.wordpress.android.ui.stats.refresh.utils.LineChartAccessibilityHelper.LineChartAccessibilityEvent
import org.wordpress.android.ui.stats.refresh.utils.LineChartLabelFormatter
import java.lang.Integer.max

@Suppress("MagicNumber")
class LineChartViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
    parent,
    R.layout.stats_block_line_chart_item
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val chart = itemView.findViewById<LineChart>(R.id.line_chart)
    private lateinit var accessibilityHelper: LineChartAccessibilityHelper

    fun bind(item: LineChartItem) {
        chart.setNoDataText("")

        coroutineScope.launch {
            delay(50)
            chart.draw(item)
            if (hasData(item.entries)) {
                chart.post {
                    val accessibilityEvent = object : LineChartAccessibilityEvent {
                        override fun onHighlight(
                            entry: Entry,
                            index: Int
                        ) {
                            drawChartMarker(Highlight(entry.x, entry.y, 0), item.selectedType)
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

    private fun LineChart.draw(item: LineChartItem) {
        resetChart()

        data = LineData(getData(item))

        configureChartView(item)
        configureYAxis(item)
        configureXAxis(item)
        configureDataSets(data.dataSets, item.selectedType)

        invalidate()
    }

    private fun getData(item: LineChartItem): List<ILineDataSet> {
        val prevWeekData = getPreviousWeekData(item)
        val prevWeekDataSet = if (hasData(prevWeekData)) {
            val mappedEntries = prevWeekData.mapIndexed { index, pair -> toLineEntry(pair, index) }
            LineDataSet(mappedEntries, "Previous week data")
        } else {
            buildEmptyDataSet(prevWeekData.size)
        }

        val thisWeekData = getThisWeekData(item)
        val thisWeekDataSet = if (hasData(thisWeekData)) {
            val mappedEntries = thisWeekData.mapIndexed { index, pair -> toLineEntry(pair, index) }
            LineDataSet(mappedEntries, "Current week data")
        } else {
            buildEmptyDataSet(thisWeekData.size)
        }
        item.onLineChartDrawn?.invoke(thisWeekDataSet.entryCount)

        val dataSets = mutableListOf<ILineDataSet>()
        dataSets.add(thisWeekDataSet)
        dataSets.add(prevWeekDataSet)

        return dataSets
    }

    private fun hasData(entries: List<Line>) = entries.isNotEmpty() && entries.any { it.value > 0 }

    private fun configureChartView(item: LineChartItem) {
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
                        drawChartMarker(h, item.selectedType)
                        item.onLineSelected?.invoke(e.data as? String)
                    }
                })
            } else {
                setOnChartValueSelectedListener(null)
            }
            isHighlightPerTapEnabled = isClickable
        }
    }

    private fun configureYAxis(item: LineChartItem) {
        val minYValue = 6f
        val maxYValue = item.entries.maxByOrNull { it.value }!!.value

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

    private fun configureXAxis(item: LineChartItem) {
        val thisWeekData = getThisWeekData(item)

        chart.xAxis.apply {
            granularity = 1f
            setDrawAxisLine(true)
            setDrawGridLines(false)

            if (chart.contentRect.width() > 0) {
                axisLineWidth = 4.0F

                val count = max(thisWeekData.count(), 7)
                val tickWidth = 4.0F
                val contentWidthMinusTicks = chart.contentRect.width() - (tickWidth * count.toFloat())
                setAxisLineDashedLine(
                    DashPathEffect(
                        floatArrayOf(tickWidth, (contentWidthMinusTicks / (count - 1).toFloat())),
                        0f
                    )
                )
            }

            setDrawLabels(true)
            setLabelCount(3, true)
            setAvoidFirstLastClipping(true)
            position = BOTTOM
            valueFormatter = LineChartLabelFormatter(thisWeekData)
            textColor = ContextCompat.getColor(chart.context, R.color.neutral_30)

            removeAllLimitLines()
        }
    }

    private fun drawChartMarker(h: Highlight, selectedType: Int) {
        if (chart.marker == null) {
            val markerView = LineChartMarkerView(chart.context, selectedType)
            markerView.chartView = chart
            chart.marker = markerView
        }
        chart.highlightValue(h)
    }

    private fun configureDataSets(dataSets: MutableList<ILineDataSet>, selectedType: Int) {
        val thisWeekDataSet = dataSets.first() as? LineDataSet
        thisWeekDataSet?.apply {
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
            color = ContextCompat.getColor(chart.context, SelectedType.getColor(selectedType))

            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(chart.context, SelectedType.getFillDrawable(selectedType))?.apply {
                alpha = 26
            }
        }

        val lastWeekDataSet = dataSets.last() as? LineDataSet
        lastWeekDataSet?.apply {
            axisDependency = LEFT

            lineWidth = 2f

            mode = CUBIC_BEZIER
            cubicIntensity = 0.2f

            color = ContextCompat.getColor(chart.context, R.color.gray_10)

            setDrawValues(false)
            setDrawCircles(false)
            isHighlightEnabled = false
        }
    }

    private fun getPreviousWeekData(item: LineChartItem): List<Line> =
        if (hasData(item.entries) && item.entries.size > 7) {
            item.entries.subList(1, 8)
        } else {
            item.entries
        }

    private fun getThisWeekData(item: LineChartItem): List<Line> =
        if (hasData(item.entries) && item.entries.size > 7) {
            item.entries.subList(8, item.entries.size)
        } else {
            emptyList()
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

    private fun <T> takeEntriesWithinGraphWidth(entries: List<T>): List<T> {
        return if (8 < entries.size) entries.subList(
            entries.size - 8,
            entries.size
        ) else {
            entries
        }
    }

    private fun LineChart.resetChart() {
        fitScreen()
        data?.clearValues()
        xAxis.valueFormatter = null
        notifyDataSetChanged()
        clear()
        invalidate()
    }

    private fun toLineEntry(line: Line, index: Int): Entry {
        return Entry(
            index.toFloat(),
            line.value.toFloat(),
            line.id
        )
    }

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
