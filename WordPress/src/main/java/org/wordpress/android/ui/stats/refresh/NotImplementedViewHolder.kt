package org.wordpress.android.ui.stats.refresh

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.layout

class NotImplementedViewHolder(
    parent: ViewGroup
) : StatsViewHolder(
        parent,
        layout.stats_not_implemented_block
) {
    private val title: TextView = itemView.findViewById(R.id.not_implemented_block_title)
    fun bind(statsItem: NotImplemented) {
        title.text = statsItem.text
    }
}
