package org.wordpress.android.ui.stats.refresh.utils

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem.Bar
import javax.inject.Inject

class BarChartLabelFormatter @Inject constructor(
    val entries: List<Bar>
) : ValueFormatter() {
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        val index = value.toInt()
        return if (index in 0..entries.size) {
            entries[index].label
        } else {
            ""
        }
    }
}
