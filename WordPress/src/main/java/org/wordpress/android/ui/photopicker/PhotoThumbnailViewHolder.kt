package org.wordpress.android.ui.photopicker

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R

/*
 * ViewHolder containing a device thumbnail
 */
class PhotoThumbnailViewHolder(parent: ViewGroup, private val thumbnailViewUtils: ThumbnailViewUtils) :
        ThumbnailViewHolder(parent, R.layout.photo_picker_thumbnail) {
    private val imgThumbnail: ImageView = itemView.findViewById(R.id.image_thumbnail)
    private val txtSelectionCount: TextView = itemView.findViewById(R.id.text_selection_count)

    fun bind(item: PhotoPickerUiItem.PhotoItem, animateSelection: Boolean, updateCount: Boolean) {
        val isSelected = item.isSelected
        thumbnailViewUtils.setupTextSelectionCount(
                txtSelectionCount,
                isSelected,
                item.selectedOrder,
                item.showOrderCounter,
                animateSelection
        )
        // Only count is updated so do not redraw the whole item
        if (updateCount) {
            return
        }
        thumbnailViewUtils.setupThumbnailImage(
                imgThumbnail,
                item.uri.toString(),
                item.isSelected,
                item.clickAction,
                item.toggleAction,
                animateSelection
        )
    }
}
