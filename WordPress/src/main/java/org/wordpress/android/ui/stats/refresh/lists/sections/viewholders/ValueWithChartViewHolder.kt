package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
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
        if (item.extraBottomMargin) {
            itemView.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = itemView.resources.getDimension(R.dimen.margin_medium).toInt()
            }
        }
        drawChart(item)
    }

    private fun drawChart(item: ValueWithChartItem) {
        chart.apply {
            if (item.chartValues == null) {
                isVisible = false
            } else {
                description.isEnabled = false
                xAxis.isEnabled = false
                axisLeft.isEnabled = false
                axisRight.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(false)
                setViewPortOffsets(0f, 0f, 0f, 0f)
                val entries = item.chartValues.mapIndexed { index, value -> Entry(index.toFloat(), value.toFloat()) }
                val lineChartColor = if (item.positive == true) {
                    R.color.green_40
                } else {
                    R.color.blue_50
                }
                val dataSet = LineDataSet(entries, null).apply {
                    setDrawCircles(false)
                    setDrawValues(false)
                    color = ContextCompat.getColor(context, lineChartColor)
                    lineWidth = 2f
                    mode = CUBIC_BEZIER
                    cubicIntensity = CUBIC_INTENSITY
                    fillBelowLine(this, item)
                }
                data = LineData(dataSet)
                isVisible = true
            }
        }
    }

    private fun fillBelowLine(dataSet: LineDataSet, item: ValueWithChartItem) {
        if (item.chartValues?.sum() == 0L) {
            return
        } else {
            val drawableRes = if (item.positive == true) {
                R.drawable.bg_rectangle_total_stats_line_chart_green_gradient
            } else {
                R.drawable.bg_rectangle_total_stats_line_chart_blue_gradient
            }
            val drawable = ContextCompat.getDrawable(chart.context, drawableRes)
            drawable?.alpha = FILL_ALPHA

            dataSet.setDrawFilled(true)
            dataSet.fillDrawable = drawable
        }
    }

    companion object {
        private const val FILL_ALPHA = 26
        private const val CUBIC_INTENSITY = 0.14f // Higher values cause visual crop at min and max values
    }
}
