package org.wordpress.android.ui.photopicker

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

@Suppress("DEPRECATION")
@Deprecated(
    "This class is being refactored, if you implement any change, please also update " +
            "{@link org.wordpress.android.ui.mediapicker.PhotoThumbnailViewHolder}"
)
class PhotoThumbnailViewHolder(
    parent: ViewGroup,
    private val thumbnailViewUtils: ThumbnailViewUtils,
    private val imageManager: ImageManager
) : ThumbnailViewHolder(parent, R.layout.photo_picker_thumbnail) {
    private val imgThumbnail: ImageView = itemView.findViewById(R.id.image_thumbnail)
    private val txtSelectionCount: TextView = itemView.findViewById(R.id.text_selection_count)

    @Suppress("DEPRECATION")
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
        imageManager.cancelRequestAndClearImageView(imgThumbnail)
        imageManager.load(
            imgThumbnail,
            PHOTO,
            item.uri.toString(),
            FIT_CENTER
        )
        thumbnailViewUtils.setupListeners(
            imgThumbnail,
            false,
            item.isSelected,
            item.toggleAction,
            item.clickAction,
            animateSelection
        )
    }
}
