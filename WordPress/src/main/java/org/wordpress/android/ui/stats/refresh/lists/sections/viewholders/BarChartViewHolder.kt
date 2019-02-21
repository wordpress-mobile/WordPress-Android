package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.TextView
import com.github.mikephil.charting.charts.BarChart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.utils.draw

class BarChartViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        layout.stats_block_bar_chart_item
) {
    private val chart = itemView.findViewById<BarChart>(id.chart)
    private val labelStart = itemView.findViewById<TextView>(id.label_start)
    private val labelEnd = itemView.findViewById<TextView>(id.label_end)

    fun bind(
        item: BarChartItem,
        barSelected: Boolean
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            delay(50)
            chart.draw(item, labelStart, labelEnd)
        }
    }
}
