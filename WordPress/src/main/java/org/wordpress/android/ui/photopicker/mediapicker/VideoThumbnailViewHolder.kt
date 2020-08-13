package org.wordpress.android.ui.photopicker.mediapicker

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.id

/*
 * ViewHolder containing a device thumbnail
 */
class VideoThumbnailViewHolder(parent: ViewGroup, private val mediaThumbnailViewUtils: MediaThumbnailViewUtils) :
        ThumbnailViewHolder(
                parent,
                R.layout.photo_picker_thumbnail
        ) {
    private val imgThumbnail: ImageView = itemView.findViewById(id.image_thumbnail)
    private val txtSelectionCount: TextView = itemView.findViewById(id.text_selection_count)
    private val videoOverlay: ImageView = itemView.findViewById(id.image_video_overlay)

    fun bind(item: MediaPickerUiItem.VideoItem, animateSelection: Boolean, updateCount: Boolean) {
        val isSelected = item.isSelected
        mediaThumbnailViewUtils.setupTextSelectionCount(
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
        mediaThumbnailViewUtils.setupThumbnailImage(
                imgThumbnail,
                item.uri.toString(),
                item.isSelected,
                item.clickAction,
                item.toggleAction,
                animateSelection
        )
        mediaThumbnailViewUtils.setupVideoOverlay(videoOverlay, item.clickAction)
    }
}
