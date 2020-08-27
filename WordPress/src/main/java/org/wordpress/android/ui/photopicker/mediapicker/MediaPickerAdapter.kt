package org.wordpress.android.ui.photopicker.mediapicker

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.photopicker.mediapicker.MediaPickerAdapterDiffCallback.Payload.COUNT_CHANGE
import org.wordpress.android.ui.photopicker.mediapicker.MediaPickerAdapterDiffCallback.Payload.SELECTION_CHANGE
import org.wordpress.android.ui.photopicker.mediapicker.MediaPickerUiItem.PhotoItem
import org.wordpress.android.ui.photopicker.mediapicker.MediaPickerUiItem.Type
import org.wordpress.android.ui.photopicker.mediapicker.MediaPickerUiItem.VideoItem
import org.wordpress.android.util.image.ImageManager

class MediaPickerAdapter internal constructor(private val imageManager: ImageManager) : Adapter<ThumbnailViewHolder>() {
    private val thumbnailViewUtils = MediaThumbnailViewUtils(imageManager)
    private var mediaList = listOf<MediaPickerUiItem>()

    init {
        setHasStableIds(true)
    }

    fun loadData(result: List<MediaPickerUiItem>) {
        val diffResult = DiffUtil.calculateDiff(
                MediaPickerAdapterDiffCallback(mediaList, result)
        )
        mediaList = result
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int {
        return mediaList.size
    }

    override fun getItemId(position: Int): Long {
        return mediaList[position].id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbnailViewHolder {
        return when (viewType) {
            Type.PHOTO.ordinal -> PhotoThumbnailViewHolder(parent, thumbnailViewUtils)
            Type.VIDEO.ordinal -> VideoThumbnailViewHolder(parent, thumbnailViewUtils)
            else -> throw IllegalArgumentException("Unexpected view type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return mediaList[position].type.ordinal
    }

    override fun onBindViewHolder(
        holder: ThumbnailViewHolder,
        position: Int,
        payloads: List<Any>
    ) {
        val item = mediaList[position]
        var animateSelection = false
        var updateCount = false
        for (payload in payloads) {
            if (payload === SELECTION_CHANGE) {
                animateSelection = true
            }
            if (payload === COUNT_CHANGE) {
                updateCount = true
            }
        }
        when (item) {
            is PhotoItem -> (holder as PhotoThumbnailViewHolder).bind(item, animateSelection, updateCount)
            is VideoItem -> (holder as VideoThumbnailViewHolder).bind(item, animateSelection, updateCount)
        }
    }

    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
        onBindViewHolder(holder, position, listOf())
    }
}
