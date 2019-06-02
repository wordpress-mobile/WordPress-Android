package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.ImageView
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ImageItem
import org.wordpress.android.util.image.ImageManager

class ImageItemViewHolder(parent: ViewGroup, val imageManager: ImageManager) : BlockListItemViewHolder(
        parent,
        layout.stats_block_image_item
) {
    private val image = itemView.findViewById<ImageView>(id.image)
    fun bind(item: ImageItem) {
        imageManager.load(image, item.imageResource)
    }
}
