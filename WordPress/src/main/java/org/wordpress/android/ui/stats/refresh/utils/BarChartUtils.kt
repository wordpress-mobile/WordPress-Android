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
import org.wordpress.android.R.color
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem.Bar
import org.wordpress.android.util.DisplayUtils
import kotlin.math.round

fun BarChart.draw(
    item: BarChartItem,
    labelStart: TextView,
    labelEnd: TextView
) {
    val graphWidth = DisplayUtils.pxToDp(context, width)
    val columnNumber = (graphWidth / 24) - 1
    val cut = cutEntries(columnNumber, item)
    val mappedEntries = cut.mapIndexed { index, pair ->
        BarEntry(
                index.toFloat(),
                pair.value.toFloat(),
                pair.id
        )
    }
    val maxYValue = cut.maxBy { it.value }!!.value
    val dataSet = getDataSet(context, mappedEntries)
    data = BarData(dataSet)
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
        if (maxYValue < 5f) {
            axisMaximum = 5f
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
    var highlightedItem = item.selectedItem

    val isClickable = item.onBarSelected != null
    if (isClickable) {
        setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onNothingSelected() {
                item.onBarSelected?.invoke(null)
                highlightValue(0f, -1, false)
                highlightedItem = null
            }

            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val value = (e as? BarEntry)?.data as? String
                if (highlightedItem != null && highlightedItem == value) {
                    onNothingSelected()
                } else {
                    item.onBarSelected?.invoke(value)
                }
                highlightedItem = null
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
            val high = Highlight(index.toFloat(), 0, 0)
            high.dataIndex = index
            highlightValue(high, false)
        } else {
            highlightValue(0f, -1, false)
        }
    }
    invalidate()
}

private fun getDataSet(context: Context, cut: List<BarEntry>): BarDataSet {
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
