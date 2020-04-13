package org.wordpress.android.imageeditor.preview

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.imageeditor.R
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageData
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState
import org.wordpress.android.imageeditor.utils.UiHelpers

class PreviewImageViewHolder(
    internal val parent: ViewGroup,
    private val loadIntoImageViewWithResultListener: (ImageData, ImageView, Int) -> Unit
) : ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.preview_image_layout, parent, false)) {
    private val container = itemView.findViewById<ConstraintLayout>(R.id.container)
    private val previewImageView = itemView.findViewById<ImageView>(R.id.previewImageView)
    private val progressBar = itemView.findViewById<ProgressBar>(R.id.progressBar)
    private val errorLayout = itemView.findViewById<ConstraintLayout>(R.id.errorLayout)
    private var onItemClicked: (() -> Unit)? = null

    init {
        container.setOnClickListener {
            onItemClicked?.invoke()
        }
    }

    fun onBind(uiState: ImageUiState) {
        onItemClicked = uiState.onItemTapped
        loadIntoImageViewWithResultListener.invoke(uiState.data, previewImageView, adapterPosition)
        UiHelpers.updateVisibility(progressBar, uiState.progressBarVisible)
        UiHelpers.updateVisibility(errorLayout, uiState.retryLayoutVisible)
    }
}
