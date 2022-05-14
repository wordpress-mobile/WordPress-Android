package org.wordpress.android.ui.stats.refresh

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import javax.inject.Inject

@Suppress("MagicNumber")
@AndroidEntryPoint
class LineChartMarkerView @Inject constructor(
    context: Context
) : MarkerView(context, R.layout.stats_line_chart_marker) {
    @Inject lateinit var statsUtils: StatsUtils
    private val changeView = findViewById<TextView>(R.id.marker_text1)
    private val countView = findViewById<TextView>(R.id.marker_text2)

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        val chart = chartView

        var thisWeekCount = 0L
        var prevWeekCount = 0L

        if (chart is LineChart) {
            val lineData = chart.lineData
            val dataSetList = lineData.dataSets // Get all the curves in the chart
            for (i in dataSetList.indices) {
                val dataSet = dataSetList[i] as LineDataSet
                // Get all the data sets on the Y axis of the curve, and
                // get the corresponding Y axis value according to the current X axis position
                val index = if (e!!.x.toInt() < dataSet.values.size) e.x.toInt() else 0
                val y = dataSet.values[index].y

                val dataSetType = dataSet.label.split(" ").last()

                val color = when (dataSetType) {
                    "Visitors" -> R.color.purple_50
                    else -> R.color.blue_50
                }

                val selectedType = when (dataSetType) {
                    "Visitors" -> context.getString(R.string.stats_visitors)
                    else -> context.getString(R.string.stats_views)
                }

                if (i == 0) {
                    thisWeekCount = y.toLong()
                    countView.text = context.getString(
                            R.string.stats_insights_views_and_visitors_tooltip_count,
                            y.toInt().toString(),
                            selectedType
                    )

                    dataSet.setDrawCircles(true)
                    dataSet.circleRadius = 8f
                    dataSet.circleColors = listOf(context.getColor(R.color.blue_0))
                    dataSet.setDrawCircleHole(true)
                    dataSet.circleHoleRadius = 4f
                    dataSet.circleHoleColor = color
                }
                if (i == 1) {
                    prevWeekCount = y.toLong()
                }
            }
            val positive = thisWeekCount >= (prevWeekCount ?: 0)
            val change = statsUtils.buildChange(prevWeekCount, thisWeekCount, positive, isFormattedNumber = false)
            changeView.text = change.toString()
        }
        super.refreshContent(e, highlight)
    }
}
