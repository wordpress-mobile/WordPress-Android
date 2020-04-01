package org.wordpress.android.imageeditor.preview

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageData

class PreviewImageThumbnailAdapter(
    private val onItemClickListener: (Int) -> Unit
) : ListAdapter<ImageData, RecyclerView.ViewHolder>(ThumbnailDiffCallback()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return PreviewImageThumbnailViewHolder.create(parent, onItemClickListener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val imageData = getItem(position)
        (holder as PreviewImageThumbnailViewHolder).onBind(imageData)
    }
}

private class ThumbnailDiffCallback : DiffUtil.ItemCallback<ImageData>() {
    override fun areItemsTheSame(oldItem: ImageData, newItem: ImageData): Boolean {
        return (oldItem.lowResImageUrl == newItem.lowResImageUrl &&
                oldItem.isSelected == newItem.isSelected)
    }

    override fun areContentsTheSame(oldItem: ImageData, newItem: ImageData): Boolean {
        return oldItem == newItem
    }
}
