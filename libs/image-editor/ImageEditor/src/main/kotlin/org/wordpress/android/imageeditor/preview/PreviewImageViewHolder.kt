package org.wordpress.android.imageeditor.preview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.imageeditor.R
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageData
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageDataStartLoadingUiState

class PreviewImageViewHolder(
    val view: View,
    private val loadIntoImageViewWithResultListener: (ImageData, ImageView) -> Unit
) : RecyclerView.ViewHolder(view) {
    private val container = itemView.findViewById<ConstraintLayout>(R.id.container)
    private val previewImageView = itemView.findViewById<ImageView>(R.id.previewImageView)
    private var onItemClicked: (() -> Unit)? = null

    init {
        container.setOnClickListener {
            onItemClicked?.invoke()
        }
    }

    fun onBind(uiState: ImageUiState) {
        onItemClicked = requireNotNull(uiState.onItemTapped) { "OnItemTapped is required." }
        if (uiState is ImageDataStartLoadingUiState) {
            loadIntoImageViewWithResultListener.invoke(uiState.imageData, previewImageView)
        }
    }

    companion object {
        fun create(
            parent: ViewGroup,
            loadIntoImageViewWithResultListener: (ImageData, ImageView) -> Unit
        ): PreviewImageViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.preview_image_layout, parent, false)
            return PreviewImageViewHolder(
                    view,
                    loadIntoImageViewWithResultListener
            )
        }
    }
}

