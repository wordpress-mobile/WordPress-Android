package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty

class EmptyViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
    parent,
    R.layout.stats_block_empty_item
) {
    private val text = itemView.findViewById<TextView>(R.id.text)
    fun bind(message: Empty) {
        when {
            message.textResource != null -> text.setText(message.textResource)
            message.text != null -> text.text = message.text
            else -> text.setText(R.string.stats_no_data_yet)
        }
    }
}
