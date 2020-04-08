package org.wordpress.android.imageeditor.preview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.imageeditor.R
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageData
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState
import org.wordpress.android.imageeditor.utils.UiHelpers

class PreviewImageViewHolder(
    val view: View,
    private val loadIntoImageViewWithResultListener: (ImageData, ImageView, Int) -> Unit
) : RecyclerView.ViewHolder(view) {
    private val previewImageView = itemView.findViewById<ImageView>(R.id.previewImageView)
    private val progressBar = itemView.findViewById<ProgressBar>(R.id.progressBar)
    private val errorLayout = itemView.findViewById<ConstraintLayout>(R.id.errorLayout)

    fun onBind(uiState: ImageUiState) {
        loadIntoImageViewWithResultListener.invoke(uiState.data, previewImageView, adapterPosition)
        UiHelpers.updateVisibility(progressBar, uiState.progressBarVisible)
        UiHelpers.updateVisibility(errorLayout, uiState.retryLayoutVisible)
    }

    companion object {
        fun create(
            parent: ViewGroup,
            loadIntoImageViewWithResultListener: (ImageData, ImageView, Int) -> Unit
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
