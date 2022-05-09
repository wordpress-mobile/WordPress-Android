package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.id
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueWithChartItem

class ValueWithChartViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        R.layout.stats_block_value_with_chart_item
) {
    private val value = itemView.findViewById<TextView>(id.value)
    fun bind(item: ValueWithChartItem) {
        value.text = item.value
        // TODO: set the chart
    }
}
