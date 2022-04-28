package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.content.Context
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
import com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT
import com.github.mikephil.charting.data.BarEntry
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
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.ViewsAndVisitorsMapper.SelectedType.Visitors
import org.wordpress.android.ui.stats.refresh.utils.LargeValueFormatter
import org.wordpress.android.ui.stats.refresh.utils.LineChartAccessibilityHelper
import org.wordpress.android.ui.stats.refresh.utils.LineChartAccessibilityHelper.LineChartAccessibilityEvent
import org.wordpress.android.ui.stats.refresh.utils.LineChartLabelFormatter

private const val MIN_VALUE = 6f

private typealias LineCount = Int

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

        val markerView = LineChartMarkerView(chart.context)
        markerView.chartView = chart
        chart.marker = markerView

        coroutineScope.launch {
            delay(50)
            val lineCount = chart.draw(item)
            chart.post {
                val accessibilityEvent = object : LineChartAccessibilityEvent {
                    override fun onHighlight(
                        entry: Entry,
                        index: Int
                    ) {
                        val value = entry.data as? String
                        value?.let {
                            item.onLineSelected?.invoke(it)
                        }
                    }
                }

                val cutContentDescriptions = takeEntriesWithinGraphWidth(lineCount, item.entryContentDescriptions)
                accessibilityHelper = LineChartAccessibilityHelper(
                        chart,
                        contentDescriptions = cutContentDescriptions,
                        accessibilityEvent = accessibilityEvent
                )

                ViewCompat.setAccessibilityDelegate(chart, accessibilityHelper)
            }
        }
    }

    @Suppress("LongMethod", "ComplexMethod")
    private fun LineChart.draw(
        item: LineChartItem
    ): LineCount {
        resetChart()

        val maxYValue = item.entries.maxByOrNull { it.value }!!.value

        val hasData = item.entries.isNotEmpty() && item.entries.any { it.value > 0 }

        val prevWeekData = if (hasData && item.entries.size > 7) {
            item.entries.subList(0, 7)
        } else {
            emptyList()
        }
        val hasPrevWeekData = prevWeekData.isNotEmpty() && prevWeekData.any { it.value > 0 }
        val prevWeekDataSet = if (hasPrevWeekData) {
            val mappedEntries = prevWeekData.mapIndexed { index, pair -> toLineEntry(pair, index) }
            buildPreviousWeekDataSet(context, mappedEntries)
        } else {
            buildEmptyDataSet(context, item.entries.size)
        }

        val thisWeekData = if (hasData && item.entries.size > 7) {
            item.entries.subList(7, item.entries.size)
        } else {
            emptyList()
        }

        val hasThisWeekData = thisWeekData.isNotEmpty() && thisWeekData.any { it.value > 0 }
        val thisWeekDataSet = if (hasThisWeekData) {
            val mappedEntries = thisWeekData.mapIndexed { index, pair -> toLineEntry(pair, index) }
            buildDataSet(context, item.selectedType, mappedEntries)
        } else {
            buildEmptyDataSet(context, item.entries.size)
        }
        item.onLineChartDrawn?.invoke(thisWeekDataSet.entryCount)

        val dataSets = mutableListOf<ILineDataSet>()
        dataSets.add(thisWeekDataSet)
        dataSets.add(prevWeekDataSet)
        data = LineData(dataSets)

        axisLeft.apply {
            valueFormatter = LargeValueFormatter()
            setDrawGridLines(true)
            setDrawTopYLabelEntry(true)
            setDrawZeroLine(false)
            setDrawAxisLine(false)
            granularity = 1f
            axisMinimum = 0f
            axisMaximum = if (maxYValue < MIN_VALUE) {
                MIN_VALUE
            } else {
                roundUp(maxYValue.toFloat())
            }
            setLabelCount(5, true)
            textColor = ContextCompat.getColor(context, R.color.neutral_30)
            gridColor = ContextCompat.getColor(context, R.color.stats_bar_chart_gridline)
            textSize = 10f
            gridLineWidth = 1f
        }
        extraLeftOffset = 8f
        axisRight.apply {
            setDrawGridLines(false)
            setDrawZeroLine(false)
            setDrawLabels(false)
            setDrawAxisLine(false)
        }
        xAxis.apply {
            granularity = 1f
            setDrawAxisLine(false)
            setDrawGridLines(false)

            setDrawLabels(true)
            setLabelCount(3, true)
            position = BOTTOM
            valueFormatter = LineChartLabelFormatter(thisWeekData)

            removeAllLimitLines()
        }
        setPinchZoom(false)
        setScaleEnabled(false)
        legend.isEnabled = false
        setDrawBorders(false)

        val isClickable = item.onLineSelected != null
        if (isClickable) {
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onNothingSelected() {
                    item.selectedItemPeriod
                    item.onLineSelected?.invoke(item.selectedItemPeriod)
                }

                override fun onValueSelected(e: Entry, h: Highlight) {
                    val value = (e as? BarEntry)?.data as? String
                    item.onLineSelected?.invoke(value)
                }
            })
        } else {
            setOnChartValueSelectedListener(null)
        }

        isHighlightPerDragEnabled = false
        isHighlightPerTapEnabled = isClickable
        val description = Description()
        description.text = ""
        this.description = description

        animateX(1000, Easing.EaseInSine)

        invalidate()
        return item.entries.size
    }

    private fun buildEmptyDataSet(context: Context, count: Int): LineDataSet {
        val emptyValues = (0 until count).map { index -> Entry(index.toFloat(), 1f, "empty") }
        val dataSet = LineDataSet(emptyValues, "Empty")
        dataSet.setGradientColor(
                ContextCompat.getColor(context, R.color.primary_5),
                ContextCompat.getColor(context, android.R.color.transparent)
        )
        dataSet.formLineWidth = 0f
        dataSet.setDrawValues(false)
        dataSet.isHighlightEnabled = false
        dataSet.fillAlpha = 80
        dataSet.setDrawHighlightIndicators(false) //
        return dataSet
    }

    private fun buildDataSet(context: Context, selectedType: Int, cut: List<Entry>): LineDataSet {
        val selectType = SelectedType.valueOf(selectedType).toString()
        val dataSet = LineDataSet(cut, "Current week $selectType")

        dataSet.axisDependency = LEFT

        dataSet.lineWidth = 2f
        dataSet.formLineWidth = 0f

        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(false)
        dataSet.setDrawHighlightIndicators(false)

        dataSet.mode = CUBIC_BEZIER
        dataSet.cubicIntensity = 0.2f
        dataSet.color = when (SelectedType.valueOf(selectedType)) {
            Visitors -> ContextCompat.getColor(context, R.color.purple_50)
            else -> ContextCompat.getColor(context, R.color.blue_50)
        }

        dataSet.setDrawFilled(true)
        dataSet.fillDrawable = when (SelectedType.valueOf(selectedType)) {
            Visitors -> ContextCompat.getDrawable(context, R.drawable.bg_rectangle_stats_line_chart_purple_gradient)
            else -> ContextCompat.getDrawable(context, R.drawable.bg_rectangle_stats_line_chart_blue_gradient)
        }

        return dataSet
    }

    private fun buildPreviousWeekDataSet(context: Context, entries: List<Entry>): LineDataSet {
        val dataSet = LineDataSet(entries, "Previous week data")
        dataSet.axisDependency = LEFT

        dataSet.lineWidth = 2f

        dataSet.mode = CUBIC_BEZIER
        dataSet.cubicIntensity = 0.2f

        dataSet.color = ContextCompat.getColor(context, R.color.gray_10)

        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(false)

        return dataSet
    }

    private fun <T> takeEntriesWithinGraphWidth(
        count: Int,
        entries: List<T>
    ): List<T> {
        return if (count < entries.size) entries.subList(
                entries.size - count,
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
