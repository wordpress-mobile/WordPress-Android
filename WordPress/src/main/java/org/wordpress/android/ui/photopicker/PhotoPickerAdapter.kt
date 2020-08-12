package org.wordpress.android.ui.photopicker

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.photopicker.PhotoPickerAdapterDiffCallback.Payload.COUNT_CHANGE
import org.wordpress.android.ui.photopicker.PhotoPickerAdapterDiffCallback.Payload.SELECTION_CHANGE
import org.wordpress.android.ui.photopicker.PhotoPickerUiItem.PhotoItem
import org.wordpress.android.ui.photopicker.PhotoPickerUiItem.Type
import org.wordpress.android.ui.photopicker.PhotoPickerUiItem.VideoItem
import org.wordpress.android.util.image.ImageManager

class PhotoPickerAdapter internal constructor(private val imageManager: ImageManager) : Adapter<ThumbnailViewHolder>() {
    private val thumbnailViewUtils = ThumbnailViewUtils(imageManager)
    private var mediaList = listOf<PhotoPickerUiItem>()

    init {
        setHasStableIds(true)
    }

    fun loadData(result: List<PhotoPickerUiItem>) {
        val diffResult = DiffUtil.calculateDiff(
                PhotoPickerAdapterDiffCallback(mediaList, result)
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
