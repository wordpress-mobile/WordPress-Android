package org.wordpress.android.imageeditor

import android.widget.ImageView
import android.widget.ImageView.ScaleType

class ImageEditor private constructor(
    private val loadImageUrlIntoImageView: ((String, ImageView, ScaleType) -> Unit)
) {
    fun loadUrlIntoImageView(imageUrl: String, imageView: ImageView, scaleType: ScaleType) {
        loadImageUrlIntoImageView.invoke(imageUrl, imageView, scaleType)
    }

    companion object {
        private lateinit var INSTANCE: ImageEditor

        val instance: ImageEditor get() = INSTANCE

        fun init(loadImageUrlIntoImageView: ((String, ImageView, ScaleType) -> Unit)) {
            INSTANCE = ImageEditor(loadImageUrlIntoImageView)
        }
    }
}
