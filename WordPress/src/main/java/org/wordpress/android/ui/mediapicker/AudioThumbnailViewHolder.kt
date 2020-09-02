package org.wordpress.android.ui.mediapicker

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R

/*
 * ViewHolder containing a device thumbnail
 */
class AudioThumbnailViewHolder(parent: ViewGroup, private val mediaThumbnailViewUtils: MediaThumbnailViewUtils) :
        ThumbnailViewHolder(parent, R.layout.media_picker_file_item) {
    private val imgThumbnail: ImageView = itemView.findViewById(R.id.media_item_filetype_image)
    private val txtSelectionCount: TextView = itemView.findViewById(R.id.text_selection_count)

    fun bind(item: MediaPickerUiItem.AudioItem, animateSelection: Boolean, updateCount: Boolean) {
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
        mediaThumbnailViewUtils.setupFileImageView(
                imgThumbnail,
                item.fileName,
                item.isSelected,
                item.clickAction,
                item.toggleAction,
                animateSelection
        )
    }
}
