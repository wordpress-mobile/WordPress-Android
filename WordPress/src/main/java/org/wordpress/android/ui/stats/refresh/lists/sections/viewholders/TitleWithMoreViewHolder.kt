package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import org.wordpress.android.R
import org.wordpress.android.R.id
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TitleWithMore

class TitleWithMoreViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        R.layout.stats_block_title_with_more_item
) {
    private val title = itemView.findViewById<TextView>(id.text)
    private val viewMore = itemView.findViewById<MaterialButton>(id.view_more_button)
    fun bind(item: TitleWithMore) {
        title.setTextOrHide(item.textResource, item.text)
        if (item.menuAction != null) {
            viewMore.visibility = View.VISIBLE
            viewMore.setOnClickListener { item.menuAction.invoke(viewMore) }
        } else {
            viewMore.visibility = View.GONE
            viewMore.setOnClickListener(null)
        }
    }
}
