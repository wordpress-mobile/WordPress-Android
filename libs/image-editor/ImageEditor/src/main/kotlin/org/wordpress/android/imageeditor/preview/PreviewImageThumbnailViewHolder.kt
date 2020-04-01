package org.wordpress.android.imageeditor.preview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.preview_image_thumbnail.view.*
import org.wordpress.android.imageeditor.ImageEditor
import org.wordpress.android.imageeditor.R
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageData

class PreviewImageThumbnailViewHolder(
    val view: View,
    private val onClickListener: (Int) -> Unit
) : RecyclerView.ViewHolder(view) {
    private val thumbnailLayout = view.findViewById<FrameLayout>(R.id.thumbnailLayout)
    private val thumbnailImageView = view.findViewById<ImageView>(R.id.thumbnailImageView)

    init {
        view.thumbnailLayout.setOnClickListener {
            onClickListener.invoke(adapterPosition)
        }
    }

    fun onBind(imageData: ImageData) {
        thumbnailLayout.isSelected = imageData.isSelected
        ImageEditor.instance.loadIntoImageView(imageData.lowResImageUrl, thumbnailImageView, ScaleType.CENTER_CROP)
    }

    companion object {
        fun create(
            parent: ViewGroup,
            onClickListener: (Int) -> Unit
        ): PreviewImageThumbnailViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.preview_image_thumbnail, parent, false)
            return PreviewImageThumbnailViewHolder(view, onClickListener)
        }
    }
}
