package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.id
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TitleNew

class TitleNewViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        R.layout.stats_block_title_item_new
) {
    private val text = itemView.findViewById<TextView>(id.text)
    private val moreButton = itemView.findViewById<TextView>(id.more_button)
    fun bind(item: TitleNew) {
        text.setTextOrHide(item.textResource, item.text)
        if (item.moreAction != null) {
            moreButton.visibility = View.VISIBLE
            moreButton.setOnClickListener { item.moreAction.invoke(moreButton) }
        } else {
            moreButton.visibility = View.GONE
            moreButton.setOnClickListener(null)
        }
    }
}
