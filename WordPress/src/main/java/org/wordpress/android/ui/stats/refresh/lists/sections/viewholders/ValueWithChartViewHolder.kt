package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER
import org.wordpress.android.R
import org.wordpress.android.R.id
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueWithChartItem

class ValueWithChartViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        R.layout.stats_block_value_with_chart_item
) {
    private val value = itemView.findViewById<TextView>(id.value)
    private val chart = itemView.findViewById<LineChart>(id.line_chart)

    fun bind(item: ValueWithChartItem) {
        value.text = item.value
        drawChart(item)
    }

    private fun drawChart(item: ValueWithChartItem) {
        chart.apply {
            if (item.values == null) {
                isVisible = false
            } else {
                description.isEnabled = false
                xAxis.isEnabled = false
                axisLeft.isEnabled = false
                axisRight.isEnabled = false
                legend.isEnabled = false
                val entries = item.values.mapIndexed { index, value -> Entry(index.toFloat(), value.toFloat()) }
                val lineChartColor = if (item.positive == true) {
                    R.color.green_40
                } else {
                    R.color.blue_50
                }
                val drawableRes = if (item.positive == true) {
                    R.drawable.bg_rectangle_total_stats_line_chart_green_gradient
                } else {
                    R.drawable.bg_rectangle_total_stats_line_chart_blue_gradient
                }
                val dataSet = LineDataSet(entries, null).apply {
                    setDrawCircles(false)
                    setDrawValues(false)
                    color = ContextCompat.getColor(context, lineChartColor)
                    lineWidth = 2f
                    mode = CUBIC_BEZIER
                    setDrawFilled(true)
                    val drawable = ContextCompat.getDrawable(context, drawableRes)
                    drawable?.alpha = FILL_ALPHA
                    fillDrawable = drawable
                }
                data = LineData(dataSet)
                isVisible = true
            }
        }
    }

    companion object {
        private const val FILL_ALPHA = 26
    }
}
