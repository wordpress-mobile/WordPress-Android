package org.wordpress.android.ui.stats.refresh.utils

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.SubscribersChartItem.Line
import javax.inject.Inject

class SubscribersChartLabelFormatter @Inject constructor(
    val entries: List<Line>
) : ValueFormatter() {
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        val index = value.toInt()
        return if (entries.isNotEmpty() && index in entries.indices) {
            entries[index].label
        } else {
            ""
        }
    }
}
