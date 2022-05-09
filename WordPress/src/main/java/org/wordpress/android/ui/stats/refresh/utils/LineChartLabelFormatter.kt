package org.wordpress.android.ui.stats.refresh.utils

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.LineChartItem.Line

class LineChartLabelFormatter(val entries: List<Line>) : ValueFormatter() {
    @Suppress("MagicNumber")
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        val index = value.toInt()
        return if (index < entries.size) {
            entries[index].label.dropLast(5)
        } else {
            ""
        }
    }
}
