package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.util.ColorUtils

class LinkViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        layout.stats_block_link_item
) {
    private val text = itemView.findViewById<TextView>(id.text)
    private val link = itemView.findViewById<View>(id.link_wrapper)

    fun bind(item: Link) {
        if (item.icon != null) {
            val drawable = ColorUtils.applyTintToDrawable(text.context, item.icon, R.color.blue_medium)
            text.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
        } else {
            text.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
        text.setText(item.text)
        link.setOnClickListener { item.navigateAction.click() }
    }
}
