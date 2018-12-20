package org.wordpress.android.ui.stats.refresh.utils

import android.content.Context
import android.support.v4.content.ContextCompat
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
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ViewPortHandler
import org.wordpress.android.R
import org.wordpress.android.R.color
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem.Bar
import org.wordpress.android.util.DisplayUtils
import kotlin.math.round

private const val MIN_COLUMN_COUNT = 5
private const val MIN_VALUE = 5f

fun BarChart.draw(
    item: BarChartItem,
    labelStart: TextView,
    labelEnd: TextView
) {
    val graphWidth = DisplayUtils.pxToDp(context, width)
    val columnNumber = (graphWidth / 24) - 1
    val cut = cutEntries(if (columnNumber > MIN_COLUMN_COUNT) columnNumber else MIN_COLUMN_COUNT, item)
    val mappedEntries = cut.mapIndexed { index, pair ->
        BarEntry(
                index.toFloat(),
                pair.value.toFloat(),
                pair.id
        )
    }
    val maxYValue = cut.maxBy { it.value }!!.value
    val dataSet = if (item.entries.isNotEmpty() && item.entries.any { it.value > 0 }) {
        buildDataSet(context, mappedEntries)
    } else {
        buildEmptyDataSet(context, cut.size)
    }
    data = if (item.onBarSelected != null) {
        BarData(dataSet, getHighlightDataSet(context, mappedEntries))
    } else {
        BarData(dataSet)
    }
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
        setDrawZeroLine(false)
        setDrawAxisLine(false)
        granularity = 1f
        axisMinimum = 0f
        if (maxYValue < MIN_VALUE) {
            axisMaximum = MIN_VALUE
        }
        textColor = greyColor
        gridColor = lightGreyColor
        textSize = 12f
        gridLineWidth = 1f
    }
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
                highlightColumn(cut.indexOfFirst { it.id == item.selectedItem })
                item.onBarSelected?.invoke(item.selectedItem)
            }

            override fun onValueSelected(e: Entry, h: Highlight) {
                val value = (e as? BarEntry)?.data as? String
                highlightColumn(e.x.toInt())
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
            highlightColumn(index)
        } else {
            highlightValue(null, false)
        }
    }
    invalidate()
}

private fun BarChart.highlightColumn(index: Int) {
    val high = Highlight(index.toFloat(), 0, 0)
    val high2 = Highlight(index.toFloat(), 1, 0)
    high.dataIndex = index
    high2.dataIndex = index
    highlightValues(arrayOf(high2, high))
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
    return dataSet
}

private fun buildDataSet(context: Context, cut: List<BarEntry>): BarDataSet {
    val dataSet = BarDataSet(cut, "Data")
    dataSet.color = ContextCompat.getColor(
            context,
            color.blue_wordpress
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

private fun getHighlightDataSet(context: Context, cut: List<BarEntry>): BarDataSet? {
    val maxEntry = cut.maxBy { it.y } ?: return null
    val highlightedDataSet = cut.map {
        BarEntry(it.x, maxEntry.y, it.data)
    }
    val dataSet = BarDataSet(highlightedDataSet, "Highlight")
    dataSet.color = ContextCompat.getColor(
            context,
            color.transparent
    )
    dataSet.formLineWidth = 0f
    dataSet.isHighlightEnabled = true
    dataSet.highLightColor = ContextCompat.getColor(
            context,
            color.orange_highlight
    )
    dataSet.setDrawValues(false)
    dataSet.highLightAlpha = 255
    return dataSet
}

private fun cutEntries(
    count: Int,
    item: BarChartItem
): List<Bar> {
    return if (count < item.entries.size) item.entries.subList(
            item.entries.size - count,
            item.entries.size
    ) else {
        item.entries
    }
}
