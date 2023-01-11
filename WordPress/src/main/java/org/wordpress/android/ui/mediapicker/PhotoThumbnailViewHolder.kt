package org.wordpress.android.ui.mediapicker

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageView.ScaleType.FIT_CENTER
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.PHOTO

/*
 * ViewHolder containing a device thumbnail
 */
class PhotoThumbnailViewHolder(
    parent: ViewGroup,
    private val mediaThumbnailViewUtils: MediaThumbnailViewUtils,
    private val imageManager: ImageManager
) : ThumbnailViewHolder(parent, R.layout.media_picker_thumbnail_item) {
    private val imgThumbnail: ImageView = itemView.findViewById(R.id.image_thumbnail)
    private val txtSelectionCount: TextView = itemView.findViewById(R.id.text_selection_count)

    fun bind(item: MediaPickerUiItem.PhotoItem, animateSelection: Boolean, updateCount: Boolean) {
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
        imageManager.cancelRequestAndClearImageView(imgThumbnail)
        imageManager.load(
            imgThumbnail,
            PHOTO,
            item.url,
            FIT_CENTER
        )
        imgThumbnail.apply {
            contentDescription = resources.getString(R.string.photo_picker_image_thumbnail_content_description)
        }
        mediaThumbnailViewUtils.setupListeners(
            imgThumbnail,
            false,
            item.isSelected,
            item.toggleAction,
            item.clickAction,
            animateSelection
        )
    }
}
