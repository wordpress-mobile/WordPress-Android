package org.wordpress.android.ui.photopicker

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.util.image.ImageManager

@Suppress("DEPRECATION")
@Deprecated(
    "This class is being refactored, if you implement any change, please also update " +
            "{@link org.wordpress.android.ui.mediapicker.MedaPickerAdapter}"
)
class PhotoPickerAdapter internal constructor(
    private val imageManager: ImageManager,
    private val coroutineScope: CoroutineScope
) : Adapter<ThumbnailViewHolder>() {
    @Suppress("DEPRECATION")
    private val thumbnailViewUtils = ThumbnailViewUtils(imageManager)

    @Suppress("DEPRECATION")
    private var mediaList = listOf<PhotoPickerUiItem>()

    init {
        setHasStableIds(true)
    }

    @Suppress("DEPRECATION")
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

    @Suppress("DEPRECATION")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbnailViewHolder {
        return when (viewType) {
            PhotoPickerUiItem.Type.PHOTO.ordinal -> PhotoThumbnailViewHolder(
                parent,
                thumbnailViewUtils,
                imageManager
            )
            PhotoPickerUiItem.Type.VIDEO.ordinal -> VideoThumbnailViewHolder(
                parent,
                thumbnailViewUtils,
                imageManager,
                coroutineScope
            )
            else -> throw IllegalArgumentException("Unexpected view type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return mediaList[position].type.ordinal
    }

    @Suppress("DEPRECATION")
    override fun onBindViewHolder(
        holder: ThumbnailViewHolder,
        position: Int,
        payloads: List<Any>
    ) {
        val item = mediaList[position]
        var animateSelection = false
        var updateCount = false
        for (payload in payloads) {
            if (payload === PhotoPickerAdapterDiffCallback.Payload.SELECTION_CHANGE) {
                animateSelection = true
            }
            if (payload === PhotoPickerAdapterDiffCallback.Payload.COUNT_CHANGE) {
                updateCount = true
            }
        }
        when (item) {
            is PhotoPickerUiItem.PhotoItem -> (holder as PhotoThumbnailViewHolder).bind(
                item,
                animateSelection,
                updateCount
            )
            is PhotoPickerUiItem.VideoItem -> (holder as VideoThumbnailViewHolder).bind(
                item,
                animateSelection,
                updateCount
            )
        }
    }

    @Suppress("DEPRECATION")
    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
        onBindViewHolder(holder, position, listOf())
    }
}
