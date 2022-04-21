package org.wordpress.android.ui.stats.refresh

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.ui.stats.refresh.utils.TEN_THOUSAND
import java.text.DecimalFormat
import java.util.Locale
import java.util.TreeMap
import javax.inject.Inject
import kotlin.math.abs

@Suppress("MagicNumber")
class LineChartMarkerView @Inject constructor(
    context: Context
) : MarkerView(context, R.layout.stats_line_chart_marker) {
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

                if (i == 0) {
                    thisWeekCount = y.toLong()
                    countView.text = context.getString(
                            R.string.stats_insights_views_and_visitors_tooltip_count,
                            y.toInt().toString(),
                            dataSetType
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
            val change = buildChange(prevWeekCount, thisWeekCount, positive, isFormattedNumber = false)
            changeView.text = change.toString()
        }
        super.refreshContent(e, highlight)
    }

    private fun buildChange(
        previousValue: Long?,
        value: Long,
        positive: Boolean,
        isFormattedNumber: Boolean
    ): String? {
        return previousValue?.let {
            val difference = value - previousValue
            val percentage = when (previousValue) {
                value -> "0"
                0L -> "∞"
                else -> mapLongToString((difference * 100 / previousValue), isFormattedNumber)
            }
            val formattedDifference = mapLongToString(difference, isFormattedNumber)
            if (positive) {
                context.getString(R.string.stats_traffic_increase, formattedDifference, percentage)
            } else {
                context.getString(R.string.stats_traffic_change, formattedDifference, percentage)
            }
        }
    }

    private fun mapLongToString(value: Long, isFormattedNumber: Boolean): String {
        return when (isFormattedNumber) {
            true -> toFormattedString(value)
            false -> value.toString()
        }
    }

    fun toFormattedString(
        number: Long,
        startValue: Int = TEN_THOUSAND
    ): String {
        val suffixes = TreeMap(
                mapOf(
                        1_000L to string.suffix_1_000,
                        1_000_000L to string.suffix_1_000_000,
                        1_000_000_000L to string.suffix_1_000_000_000,
                        1_000_000_000_000L to string.suffix_1_000_000_000_000,
                        1_000_000_000_000_000L to string.suffix_1_000_000_000_000_000,
                        1_000_000_000_000_000_000L to string.suffix_1_000_000_000_000_000_000
                )
        )
        val isNegative = number < 0
        val safeNumber = abs(
                if (number == java.lang.Long.MIN_VALUE) {
                    number + 1
                } else {
                    number
                }
        )
        if (safeNumber < startValue) {
            return printNumber(safeNumber, isNegative)
        }

        val e = suffixes.floorEntry(safeNumber)
        val divideBy = e?.key
        val suffix = e?.value

        val truncated = safeNumber / (divideBy!! / 10)
        val hasDecimal = truncated < 100 && truncated / 10.0 != (truncated / 10).toDouble()
        return printNumber(truncated, isNegative, suffix, hasDecimal)
    }

    private fun printNumber(
        number: Long,
        isNegative: Boolean,
        suffix: Int? = null,
        hasDecimal: Boolean = false
    ): String {
        val formattedNumber =
                DecimalFormat.getInstance(Locale.getDefault()).format(
                        when {
                            suffix != null && hasDecimal -> number / 10.0
                            suffix != null -> number / 10
                            else -> number
                        }
                )
        return if (suffix != null) {
            context.getString(
                    suffix,
                    handleNegativeNumber(
                            formattedNumber,
                            isNegative
                    )
            )
        } else {
            handleNegativeNumber(
                    formattedNumber,
                    isNegative
            )
        }
    }

    private fun handleNegativeNumber(number: String, isNegative: Boolean): String {
        return if (isNegative) {
            context.getString(R.string.negative_prefix, number)
        } else {
            number
        }
    }
}
