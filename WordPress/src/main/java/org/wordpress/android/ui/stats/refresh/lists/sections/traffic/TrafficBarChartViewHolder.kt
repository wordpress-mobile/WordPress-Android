package org.wordpress.android.ui.stats.refresh.lists.sections.traffic

import android.content.Context
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TrafficBarChartItem.Bar
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.BlockListItemViewHolder
import org.wordpress.android.ui.stats.refresh.utils.BarChartAccessibilityHelper
import org.wordpress.android.ui.stats.refresh.utils.BarChartLabelFormatter
import org.wordpress.android.ui.stats.refresh.utils.LargeValueFormatter
import org.wordpress.android.util.DisplayUtils

@Suppress("MagicNumber")
class TrafficBarChartViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
    parent,
    R.layout.stats_block_traffic_bar_chart_item
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val chart = itemView.findViewById<BarChart>(R.id.bar_chart)

    private lateinit var accessibilityHelper: BarChartAccessibilityHelper

    fun bind(item: BlockListItem.TrafficBarChartItem) {
        chart.setNoDataText("")
        coroutineScope.launch {
            delay(50)
            val barCount = chart.draw(item)
            if (hasData(item.entries)) {
                chart.post {
                    val cutContentDescriptions = takeEntriesWithinGraphWidth(barCount, item.entryContentDescriptions)
                    accessibilityHelper = BarChartAccessibilityHelper(chart, cutContentDescriptions)

                    ViewCompat.setAccessibilityDelegate(chart, accessibilityHelper)
                }
            }
        }
    }

    private fun BarChart.draw(item: BlockListItem.TrafficBarChartItem): Int {
        resetChart()
        val dataSet = getData(item)
        val dataSets = mutableListOf<IBarDataSet>()
        dataSets.add(dataSet)

        data = BarData(dataSets)

        configureChartView()
        configureYAxis(item)
        configureXAxis(item)

        invalidate()
        return dataSet.entryCount
    }

    private fun hasData(entries: List<Bar>) = entries.isNotEmpty() && entries.any { it.value > 0 }

    private fun getData(item: BlockListItem.TrafficBarChartItem): IBarDataSet {
        val minColumnCount = 5

        val graphWidth = DisplayUtils.pxToDp(chart.context, chart.width)
        val columnNumber = (graphWidth / 24) - 1
        val count = if (columnNumber > minColumnCount) columnNumber else minColumnCount
        val cutEntries = takeEntriesWithinGraphWidth(count, item.entries)
        val mappedEntries = cutEntries.mapIndexed { index, pair -> toBarEntry(pair, index) }

        val dataSet = if (hasData(item.entries)) {
            buildDataSet(chart.context, mappedEntries)
        } else {
            buildEmptyDataSet(chart.context, cutEntries.size)
        }
        item.onBarChartDrawn?.invoke(dataSet.entryCount)

        return dataSet
    }

    private fun configureChartView() {
        chart.apply {
            setPinchZoom(false)
            setScaleEnabled(false)
            legend.isEnabled = false
            setDrawBorders(false)

            isHighlightPerDragEnabled = false
            isHighlightPerTapEnabled = isClickable
            val description = Description()
            description.text = ""
            this.description = description

            extraRightOffset = 8f

            animateY(250)
        }
    }

    private fun configureYAxis(item: BlockListItem.TrafficBarChartItem) {
        val minYValue = 4f
        val maxYValue = item.entries.maxByOrNull { it.value }?.value ?: 0

        chart.axisLeft.apply {
            setDrawGridLines(false)
            setDrawZeroLine(false)
            setDrawLabels(false)
            setDrawAxisLine(false)
            axisMinimum = 0f
            axisMaximum = if (maxYValue < minYValue) {
                minYValue
            } else {
                roundUp(maxYValue.toFloat())
            }
        }

        chart.axisRight.apply {
            valueFormatter = LargeValueFormatter()
            setDrawGridLines(true)
            setDrawTopYLabelEntry(true)
            setDrawZeroLine(false)
            setDrawAxisLine(true)
            axisMinimum = 0f
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

    private fun configureXAxis(item: BlockListItem.TrafficBarChartItem) {
        chart.xAxis.apply {
            setDrawAxisLine(false)
            setDrawGridLines(false)

            setDrawLabels(true)
            labelCount = item.entries.size
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = BarChartLabelFormatter(item.entries)
            textColor = ContextCompat.getColor(chart.context, R.color.neutral_30)
        }
    }

    private fun buildEmptyDataSet(context: Context, count: Int): BarDataSet {
        val emptyValues = (0 until count).map { index -> BarEntry(index.toFloat(), 1f, "empty") }
        val dataSet = BarDataSet(emptyValues, "Empty")
        dataSet.setGradientColor(
            ContextCompat.getColor(
                context,
                R.color.primary_5
            ), ContextCompat.getColor(
                context,
                android.R.color.transparent
            )
        )
        dataSet.formLineWidth = 0f
        dataSet.setDrawValues(false)
        dataSet.isHighlightEnabled = false
        dataSet.highLightAlpha = 255
        return dataSet
    }

    private fun buildDataSet(context: Context, cut: List<BarEntry>): BarDataSet {
        val dataSet = BarDataSet(cut, "Data")
        chart.renderer.paintRender.shader = null
        dataSet.color = ContextCompat.getColor(context, R.color.blue_50)
        dataSet.formLineWidth = 0f
        dataSet.setDrawValues(false)

        return dataSet
    }

    private fun <T> takeEntriesWithinGraphWidth(
        count: Int,
        entries: List<T>
    ): List<T> = if (count < entries.size) entries.subList(
        entries.size - count,
        entries.size
    ) else {
        entries
    }

    private fun BarChart.resetChart() {
        fitScreen()
        data?.clearValues()
        xAxis.valueFormatter = null
        notifyDataSetChanged()
        clear()
        invalidate()
    }

    private fun toBarEntry(bar: Bar, index: Int): BarEntry = BarEntry(
        index.toFloat(),
        bar.value.toFloat(),
        bar.id
    )

    @Suppress("ReturnCount")
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
            return 100F
        }
    }
}
