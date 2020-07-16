package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageView.ScaleType.CENTER
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ImageItem
import org.wordpress.android.util.image.ImageManager

class ImageItemViewHolder(parent: ViewGroup, val imageManager: ImageManager) : BlockListItemViewHolder(
        parent,
        R.layout.stats_block_image_item
) {
    private val image = itemView.findViewById<ImageView>(R.id.image)
    fun bind(item: ImageItem) {
        val vectorDrawable = VectorDrawableCompat.create(image.resources, item.imageResource, null)
        if (vectorDrawable != null) {
            imageManager.load(image, vectorDrawable, CENTER)
        }
    }
}
