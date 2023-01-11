package org.wordpress.android.ui.photopicker

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageView.ScaleType.FIT_CENTER
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.R
import org.wordpress.android.R.id
import org.wordpress.android.util.image.ImageManager

/*
 * ViewHolder containing a device thumbnail
 */

@Suppress("DEPRECATION")
@Deprecated(
    "This class is being refactored, if you implement any change, please also update " +
            "{@link org.wordpress.android.ui.mediapicker.VideoThumbnailViewHolder}"
)
class VideoThumbnailViewHolder(
    parent: ViewGroup,
    private val thumbnailViewUtils: ThumbnailViewUtils,
    private val imageManager: ImageManager,
    private val coroutineScope: CoroutineScope
) : ThumbnailViewHolder(parent, R.layout.photo_picker_thumbnail) {
    private val imgThumbnail: ImageView = itemView.findViewById(id.image_thumbnail)
    private val txtSelectionCount: TextView = itemView.findViewById(id.text_selection_count)
    private val videoOverlay: ImageView = itemView.findViewById(id.image_video_overlay)

    @Suppress("DEPRECATION")
    fun bind(item: PhotoPickerUiItem.VideoItem, animateSelection: Boolean, updateCount: Boolean) {
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
        imageManager.loadThumbnailFromVideoUrl(
            coroutineScope,
            imgThumbnail,
            item.uri.toString(),
            FIT_CENTER
        )
        thumbnailViewUtils.setupListeners(
            imgThumbnail,
            true,
            item.isSelected,
            item.toggleAction,
            item.clickAction,
            animateSelection
        )
        thumbnailViewUtils.setupVideoOverlay(videoOverlay, item.clickAction)
    }
}
