package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ReferredItem

class ReferredItemViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
    parent,
    R.layout.stats_block_referred_item
) {
    private val container = itemView.findViewById<LinearLayout>(R.id.container)
    private val label = itemView.findViewById<TextView>(R.id.label)
    private val title = itemView.findViewById<TextView>(R.id.title)
    fun bind(item: ReferredItem) {
        container.setOnClickListener { item.navigationAction?.click() }
        label.setText(item.label)
        title.text = item.itemTitle
    }
}
