package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.MapLegend

class MapLegendViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
    parent,
    R.layout.stats_block_map_legend_item
) {
    private val startLegend: TextView = itemView.findViewById(R.id.start_legend)
    private val endLegend: TextView = itemView.findViewById(R.id.end_legend)
    fun bind(item: MapLegend) {
        startLegend.text = item.startLegend
        endLegend.text = item.endLegend
    }
}
