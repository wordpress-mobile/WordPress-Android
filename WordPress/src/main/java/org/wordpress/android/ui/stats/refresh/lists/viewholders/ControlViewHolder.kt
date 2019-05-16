package org.wordpress.android.ui.stats.refresh.lists.viewholders

import android.view.ViewGroup
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.util.image.ImageManager

class ControlViewHolder(val parent: ViewGroup, imageManager: ImageManager) : BlockListViewHolder(
        parent,
        imageManager
) {
    fun bind(items: List<BlockListItem>) {
        super.bind(null, items)
        itemView.background = null
    }
}
