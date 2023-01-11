package org.wordpress.android.ui.mediapicker

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.ui.mediapicker.MediaPickerAdapterDiffCallback.Payload.COUNT_CHANGE
import org.wordpress.android.ui.mediapicker.MediaPickerAdapterDiffCallback.Payload.SELECTION_CHANGE
import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.FileItem
import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.NextPageLoader
import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.PhotoItem
import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.Type
import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.VideoItem
import org.wordpress.android.util.image.ImageManager

class MediaPickerAdapter internal constructor(
    private val imageManager: ImageManager,
    private val coroutineScope: CoroutineScope
) : Adapter<ThumbnailViewHolder>() {
    private val thumbnailViewUtils = MediaThumbnailViewUtils(imageManager)
    private var mediaList = listOf<MediaPickerUiItem>()

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbnailViewHolder {
        return when (viewType) {
            Type.PHOTO.ordinal -> PhotoThumbnailViewHolder(parent, thumbnailViewUtils, imageManager)
            Type.VIDEO.ordinal -> VideoThumbnailViewHolder(parent, thumbnailViewUtils, imageManager, coroutineScope)
            Type.FILE.ordinal -> FileThumbnailViewHolder(parent, thumbnailViewUtils)
            Type.NEXT_PAGE_LOADER.ordinal -> LoaderViewHolder(parent)
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
            is FileItem -> (holder as FileThumbnailViewHolder).bind(item, animateSelection, updateCount)
            is NextPageLoader -> (holder as LoaderViewHolder).bind(item)
        }
    }

    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
        onBindViewHolder(holder, position, listOf())
    }
}
