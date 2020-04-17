package org.wordpress.android.imageeditor.preview

import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageData
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState

class PreviewImageAdapter(
    private val loadIntoImageViewWithResultListener: (ImageData, ImageView, Int) -> Unit
) : ListAdapter<ImageUiState, RecyclerView.ViewHolder>(PreviewImageDiffCallback()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder = PreviewImageViewHolder(parent, loadIntoImageViewWithResultListener)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val uiState = getItem(position)
        (holder as PreviewImageViewHolder).onBind(uiState)
    }

    override fun getItemId(position: Int): Long {
        val uiState = getItem(position)
        return uiState.data.id
    }
}

private class PreviewImageDiffCallback : DiffUtil.ItemCallback<ImageUiState>() {
    override fun areItemsTheSame(oldItem: ImageUiState, newItem: ImageUiState): Boolean {
        return oldItem.data.id == newItem.data.id
    }

    override fun areContentsTheSame(oldItem: ImageUiState, newItem: ImageUiState): Boolean {
        return oldItem.data == newItem.data &&
            oldItem.progressBarVisible == newItem.progressBarVisible &&
            oldItem.retryLayoutVisible == newItem.retryLayoutVisible
    }
}
