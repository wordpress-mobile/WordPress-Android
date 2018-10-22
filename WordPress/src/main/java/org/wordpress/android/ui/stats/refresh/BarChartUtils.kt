package org.wordpress.android.ui.stats.refresh

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
import com.github.mikephil.charting.utils.ViewPortHandler
import org.wordpress.android.R.color
import org.wordpress.android.ui.stats.refresh.BlockListItem.BarChartItem
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
    val maxXValue = cut.maxBy { it.y }!!.y
    val dataSet = getDataSet(context, cut)
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
        if (maxXValue < 5f) {
            axisMaximum = 5f
        }
        textColor = greyColor
        gridColor = lightGreyColor
        textSize = 12f
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
    labelStart.text = cut.first().data.toString()
    labelEnd.text = cut.last().data.toString()
    setPinchZoom(false)
    setScaleEnabled(false)
    legend.isEnabled = false
    setDrawBorders(false)
    isHighlightFullBarEnabled = false
    isHighlightPerDragEnabled = false
    isHighlightPerTapEnabled = false
    val description = Description()
    description.text = ""
    this.description = description
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
    return dataSet
}

private fun cutEntries(
    count: Int,
    item: BarChartItem
): List<BarEntry> {
    return if (count < item.entries.size) item.entries.subList(
            item.entries.size - count,
            item.entries.size
    ) else {
        item.entries
    }
}
