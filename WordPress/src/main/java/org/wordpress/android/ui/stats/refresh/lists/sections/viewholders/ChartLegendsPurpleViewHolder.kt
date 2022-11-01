package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ChartLegendsPurple

class ChartLegendsPurpleViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        R.layout.stats_block_legends_purple_item
) {
    private val thisWeek = itemView.findViewById<TextView>(R.id.legend1)
    private val prevWeek = itemView.findViewById<TextView>(R.id.legend2)
    fun bind(item: ChartLegendsPurple) {
        thisWeek.setText(item.legend1)
        prevWeek.setText(item.legend2)
    }
}
