package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.ViewGroup
import android.widget.TextView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.LargeValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ViewPortHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.R.color
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem.Bar
import org.wordpress.android.util.DisplayUtils
import kotlin.math.round

private const val MIN_COLUMN_COUNT = 5
private const val MIN_VALUE = 5f

class BarChartViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        layout.stats_block_bar_chart_item
) {
    private val chart = itemView.findViewById<BarChart>(id.chart)
    private val labelStart = itemView.findViewById<TextView>(id.label_start)
    private val labelEnd = itemView.findViewById<TextView>(id.label_end)

    fun bind(
        item: BarChartItem,
        barSelected: Boolean
    ) {
        chart.setNoDataText("")
        GlobalScope.launch(Dispatchers.Main) {
            delay(50)
            chart.draw(item, labelStart, labelEnd)
        }
    }

    private fun BarChart.draw(
        item: BarChartItem,
        labelStart: TextView,
        labelEnd: TextView
    ) {
        resetChart()
        val graphWidth = DisplayUtils.pxToDp(context, width)
        val columnNumber = (graphWidth / 24) - 1
        val count = if (columnNumber > MIN_COLUMN_COUNT) columnNumber else MIN_COLUMN_COUNT
        val cut = cutEntries(count, item.entries)
        val mappedEntries = cut.mapIndexed { index, pair -> toBarEntry(pair, index) }
        val maxYValue = cut.maxBy { it.value }!!.value
        val hasData = item.entries.isNotEmpty() && item.entries.any { it.value > 0 }
        val dataSet = if (hasData) {
            buildDataSet(context, mappedEntries)
        } else {
            buildEmptyDataSet(context, cut.size)
        }
        item.onBarChartDrawn?.invoke(dataSet.entryCount)
        val dataSets = mutableListOf<IBarDataSet>()
        dataSets.add(dataSet)
        val hasOverlappingEntries = hasData && item.overlappingEntries != null
        if (hasData && item.overlappingEntries != null) {
            val overlappingCut = cutEntries(count, item.overlappingEntries)
            val mappedOverlappingEntries = overlappingCut.mapIndexed { index, pair -> toBarEntry(pair, index) }
            val overlappingDataSet = buildOverlappingDataSet(context, mappedOverlappingEntries)
            dataSets.add(overlappingDataSet)
        }
        if (hasData && item.onBarSelected != null) {
            getHighlightDataSet(context, mappedEntries)?.let { dataSets.add(it) }
        }
        data = BarData(dataSets)
        val greyColor = ContextCompat.getColor(
                context,
                color.wp_grey
        )
        val lightGreyColor = ContextCompat.getColor(
                context,
                color.wp_grey_lighten_30
        )
        axisLeft.apply {
            valueFormatter = object : LargeValueFormatter() {
                override fun getFormattedValue(value: Float, axis: AxisBase?): String {
                    return super.getFormattedValue(round(value), axis)
                }

                override fun getFormattedValue(
                    value: Float,
                    entry: Entry?,
                    dataSetIndex: Int,
                    viewPortHandler: ViewPortHandler?
                ): String {
                    return super.getFormattedValue(round(value), entry, dataSetIndex, viewPortHandler)
                }
            }
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
            textColor = greyColor
            gridColor = lightGreyColor
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
            setDrawLabels(false)
        }
        labelStart.text = cut.first().label
        labelEnd.text = cut.last().label
        setPinchZoom(false)
        setScaleEnabled(false)
        legend.isEnabled = false
        setDrawBorders(false)

        val isClickable = item.onBarSelected != null
        if (isClickable) {
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onNothingSelected() {
                    item.selectedItem
                    highlightColumn(cut.indexOfFirst { it.id == item.selectedItem }, hasOverlappingEntries)
                    item.onBarSelected?.invoke(item.selectedItem)
                }

                override fun onValueSelected(e: Entry, h: Highlight) {
                    val value = (e as? BarEntry)?.data as? String
                    highlightColumn(e.x.toInt(), hasOverlappingEntries)
                    item.onBarSelected?.invoke(value)
                }
            })
        } else {
            setOnChartValueSelectedListener(null)
        }
        isHighlightFullBarEnabled = isClickable
        isHighlightPerDragEnabled = false
        isHighlightPerTapEnabled = isClickable
        val description = Description()
        description.text = ""
        this.description = description

        if (item.selectedItem != null) {
            val index = cut.indexOfFirst { it.id == item.selectedItem }
            if (index >= 0) {
                highlightColumn(index, hasOverlappingEntries)
            } else {
                highlightValue(null, false)
            }
        }
        invalidate()
    }

    private fun BarChart.highlightColumn(index: Int, hasOverlappingColumns: Boolean) {
        if (hasOverlappingColumns) {
            val high = Highlight(index.toFloat(), 0, 0)
            val high2 = Highlight(index.toFloat(), 1, 1)
            val high3 = Highlight(index.toFloat(), 2, 2)
            high.dataIndex = index
            high2.dataIndex = index
            high3.dataIndex = index
            highlightValues(arrayOf(high3, high, high2))
        } else {
            val high = Highlight(index.toFloat(), 0, 0)
            val high2 = Highlight(index.toFloat(), 1, 1)
            high.dataIndex = index
            high2.dataIndex = index
            highlightValues(arrayOf(high2, high))
        }
    }

    private fun buildEmptyDataSet(context: Context, count: Int): BarDataSet {
        val emptyValues = (0 until count).map { index -> BarEntry(index.toFloat(), 1f, "empty") }
        val dataSet = BarDataSet(emptyValues, "Empty")
        dataSet.setGradientColor(
                ContextCompat.getColor(
                        context,
                        R.color.stats_bar_chart_gradient_start_color
                ), ContextCompat.getColor(
                context,
                R.color.transparent
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
        dataSet.color = ContextCompat.getColor(context, R.color.blue_wordpress)
        dataSet.setGradientColor(
                ContextCompat.getColor(
                        context,
                        R.color.blue_wordpress
                ), ContextCompat.getColor(
                context,
                R.color.blue_wordpress
        )
        )
        dataSet.formLineWidth = 0f
        dataSet.setDrawValues(false)
        dataSet.isHighlightEnabled = true
        dataSet.highLightColor = ContextCompat.getColor(
                context,
                color.orange_active
        )
        dataSet.highLightAlpha = 255
        return dataSet
    }

    private fun buildOverlappingDataSet(context: Context, cut: List<BarEntry>): BarDataSet {
        val dataSet = BarDataSet(cut, "Overlapping data")
        dataSet.color = ContextCompat.getColor(context, R.color.blue_dark)
        dataSet.setGradientColor(
                ContextCompat.getColor(
                        context,
                        R.color.blue_dark
                ), ContextCompat.getColor(
                context,
                R.color.blue_dark
        )
        )
        dataSet.formLineWidth = 0f
        dataSet.setDrawValues(false)
        dataSet.isHighlightEnabled = true
        dataSet.highLightColor = ContextCompat.getColor(
                context,
                color.orange_dark_active
        )
        dataSet.highLightAlpha = 255
        return dataSet
    }

    private fun getHighlightDataSet(context: Context, cut: List<BarEntry>): BarDataSet? {
        val maxEntry = cut.maxBy { it.y } ?: return null
        val highlightedDataSet = cut.map {
            BarEntry(it.x, maxEntry.y, it.data)
        }
        val dataSet = BarDataSet(highlightedDataSet, "Highlight")
        dataSet.color = ContextCompat.getColor(context, R.color.transparent)
        dataSet.setGradientColor(
                ContextCompat.getColor(
                        context,
                        R.color.transparent
                ), ContextCompat.getColor(
                context,
                R.color.transparent
        )
        )
        dataSet.formLineWidth = 0f
        dataSet.isHighlightEnabled = true
        dataSet.highLightColor = ContextCompat.getColor(
                context,
                color.orange_active
        )
        dataSet.setDrawValues(false)
        dataSet.highLightAlpha = 51
        return dataSet
    }

    private fun cutEntries(
        count: Int,
        entries: List<Bar>
    ): List<Bar> {
        return if (count < entries.size) entries.subList(
                entries.size - count,
                entries.size
        ) else {
            entries
        }
    }

    private fun BarChart.resetChart() {
        fitScreen()
        data?.clearValues()
        xAxis.valueFormatter = null
        notifyDataSetChanged()
        clear()
        invalidate()
    }

    private fun toBarEntry(bar: Bar, index: Int): BarEntry {
        return BarEntry(
                index.toFloat(),
                bar.value.toFloat(),
                bar.id
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
            return 100F
        }
    }
}
