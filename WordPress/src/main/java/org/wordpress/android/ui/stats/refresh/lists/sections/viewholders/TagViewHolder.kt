package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Tag

class TagViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
    parent,
    R.layout.stats_block_tag_item
) {
    private val tag = itemView.findViewById<TextView>(R.id.tag)
    fun bind(item: Tag) {
        tag.setText(item.textResource)
    }
}
