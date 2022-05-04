package org.wordpress.android.ui.mediapicker

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
class VideoThumbnailViewHolder(
    parent: ViewGroup,
    private val mediaThumbnailViewUtils: MediaThumbnailViewUtils,
    private val imageManager: ImageManager,
    private val coroutineScope: CoroutineScope
) : ThumbnailViewHolder(
                parent,
                R.layout.media_picker_thumbnail_item
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
        imageManager.cancelRequestAndClearImageView(imgThumbnail)
        imageManager.loadThumbnailFromVideoUrl(
                coroutineScope,
                imgThumbnail,
                item.url,
                FIT_CENTER
        )
        imgThumbnail.apply {
            contentDescription = resources.getString(R.string.photo_picker_video_thumbnail_content_description)
        }
        mediaThumbnailViewUtils.setupListeners(
                imgThumbnail,
                true,
                item.isSelected,
                item.toggleAction,
                item.clickAction,
                animateSelection
        )
        mediaThumbnailViewUtils.setupVideoOverlay(videoOverlay, item.clickAction)
    }
}
