package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ChartLegend

class ChartLegendViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        layout.stats_block_legend_item
) {
    private val legend = itemView.findViewById<TextView>(id.legend)
    fun bind(item: ChartLegend) {
        legend.setText(item.text)
    }
}
