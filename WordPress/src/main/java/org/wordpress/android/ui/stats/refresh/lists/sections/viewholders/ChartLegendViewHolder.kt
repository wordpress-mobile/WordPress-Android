package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ChartLegend

class ChartLegendViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
    parent,
    R.layout.stats_block_legend_item
) {
    private val legend = itemView.findViewById<TextView>(R.id.legend)
    fun bind(item: ChartLegend) {
        legend.setText(item.text)
    }
}
