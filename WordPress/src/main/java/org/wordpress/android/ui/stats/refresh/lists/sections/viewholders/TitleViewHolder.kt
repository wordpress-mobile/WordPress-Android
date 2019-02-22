package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title

class TitleViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        layout.stats_block_title_item
) {
    private val text = itemView.findViewById<TextView>(id.text)
    fun bind(item: Title) {
        text.setTextOrHide(item.textResource, item.text)
    }
}
