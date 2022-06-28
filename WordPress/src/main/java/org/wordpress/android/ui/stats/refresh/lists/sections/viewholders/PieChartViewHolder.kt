package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.content.res.ColorStateList
import android.text.style.AbsoluteSizeSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.view.doOnLayout
import androidx.core.widget.TextViewCompat
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import org.wordpress.android.R
import org.wordpress.android.databinding.ItemPieChartLegendBinding
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.PieChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.PieChartItem.Pie
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.widgets.RoundedSlicesPieChartRenderer
import kotlin.math.max

class PieChartViewHolder(parent: ViewGroup) : BlockListItemViewHolder(parent, R.layout.stats_block_pie_chart_item) {
    private val chart = itemView.findViewById<PieChart>(R.id.chart)
    private val totalText = itemView.findViewById<TextView>(R.id.total_text)
    private val legends = itemView.findViewById<LinearLayout>(R.id.legends)

    fun bind(item: PieChartItem) {
        val sliceWidth = chart.resources.getDimension(R.dimen.stats_pie_chart_slice_width)
        setHoleRadius(sliceWidth)

        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false

            val roundedSlicesPieChartRenderer = RoundedSlicesPieChartRenderer(this)
            renderer = roundedSlicesPieChartRenderer

            totalText.text = buildSpannedString {
                append(item.totalLabel)
                val textSize = context.resources.getDimension(R.dimen.text_sz_extra_extra_extra_large)
                inSpans(AbsoluteSizeSpan(textSize.toInt())) { append("\n${item.total}") }
            }

            val dataSet = getDataSet(item)
            dataSet.sliceSpace = DisplayUtils.pxToDp(context, sliceWidth.toInt()).toFloat()
            data = PieData(dataSet)
        }
        addLegends(item)
        itemView.contentDescription = item.contentDescription
    }

    private fun mapToPieEntry(entries: List<Pie>): List<PieEntry> {
        val sum = entries.sumOf { it.value }
        val minValue = sum * MIN_PIE_CHART_RATIO
        return entries.map { PieEntry(max(it.value.toFloat(), minValue)) }
    }

    private fun setHoleRadius(sliceWidth: Float) {
        chart.doOnLayout { chart.holeRadius = (1 - 2 * sliceWidth / chart.width) * PERCENT_HUNDRED }
    }

    private fun getDataSet(item: PieChartItem): PieDataSet {
        val pieChartEntries = item.entries.filter { it.value > 0 }
        return PieDataSet(mapToPieEntry(pieChartEntries), null).apply {
            label = null
            setDrawValues(false)
            colors = item.colors.map { ContextCompat.getColor(chart.context, it) }
            selectionShift = 0f // Needed for removing extra highlighting space in chart size
        }
    }

    private fun addLegends(item: PieChartItem) {
        legends.removeAllViews()
        item.entries.forEachIndexed { index, pie ->
            ItemPieChartLegendBinding.inflate(LayoutInflater.from(legends.context), legends, true).apply {
                val textView = root.findViewById<TextView>(R.id.text)
                textView.text = pie.label
                val colorRes = item.colors[index % item.entries.size]
                val color = ContextCompat.getColor(root.context, colorRes)
                TextViewCompat.setCompoundDrawableTintList(textView, ColorStateList.valueOf(color))
            }
        }
    }

    companion object {
        private const val PERCENT_HUNDRED = 100
        private const val MIN_PIE_CHART_RATIO = 0.039f
    }
}
