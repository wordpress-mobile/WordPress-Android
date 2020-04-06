package  org.wordpress.android.imageeditor.preview

import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageData
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageDataStartLoadingUiState

class PreviewImageAdapter(
    private val loadIntoImageViewWithResultListener: (ImageData, ImageView) -> Unit
) : ListAdapter<ImageUiState, RecyclerView.ViewHolder>(PreviewImageDiffCallback()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return PreviewImageViewHolder.create(
                parent,
                loadIntoImageViewWithResultListener
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val uiState = getItem(position)
        (holder as PreviewImageViewHolder).onBind(uiState)
    }
}

private class PreviewImageDiffCallback : DiffUtil.ItemCallback<ImageUiState>() {
    override fun areItemsTheSame(oldItem: ImageUiState, newItem: ImageUiState): Boolean {
        return when (oldItem) {
            is ImageDataStartLoadingUiState -> {
                if (newItem is ImageDataStartLoadingUiState) {
                    oldItem.imageData.lowResImageUrl == newItem.imageData.lowResImageUrl &&
                            oldItem.imageData.highResImageUrl == newItem.imageData.highResImageUrl
                } else {
                    true  // TODO: fix it
                }
            }
            else -> {
                true  // TODO: fix it
            }
        }
    }

    override fun areContentsTheSame(oldItem: ImageUiState, newItem: ImageUiState): Boolean {
        if (oldItem::class != newItem::class) {
            return false
        }
        return when (oldItem) {
            is ImageDataStartLoadingUiState -> {
                if (newItem is ImageDataStartLoadingUiState) {
                    oldItem.imageData == newItem.imageData
                } else {
                    false
                }
            }
            else -> {
                false
            }
        }
    }
}
