package org.wordpress.android.ui.photopicker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.R
import org.wordpress.android.ui.photopicker.PhotoPickerAdapterDiffCallback.Payload.COUNT_CHANGE
import org.wordpress.android.ui.photopicker.PhotoPickerAdapterDiffCallback.Payload.SELECTION_CHANGE
import org.wordpress.android.util.image.ImageManager

class PhotoPickerAdapter internal constructor(private val imageManager: ImageManager) : Adapter<ThumbnailViewHolder>() {
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
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.photo_picker_thumbnail, parent, false)
        return ThumbnailViewHolder(view, imageManager)
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
        holder.bind(item, animateSelection, updateCount)
    }

    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
        holder.bind(mediaList[position], animateSelection = false, updateCount = false)
    }
}
