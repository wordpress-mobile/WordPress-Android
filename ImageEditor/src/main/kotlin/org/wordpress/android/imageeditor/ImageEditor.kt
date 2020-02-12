package org.wordpress.android.imageeditor

import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.ImageView.ScaleType

class ImageEditor private constructor(
    private val loadImageWithResultListener: ((String, ImageView, ScaleType, String, RequestListener<Drawable>) -> Unit)

) {
    interface RequestListener<T> {
        fun onLoadFailed(e: Exception?)
        fun onResourceReady(resource: T)
    }

    fun loadImageWithResultListener(
        imageUrl: String,
        imageView: ImageView,
        scaleType: ScaleType,
        thumbUrl: String,
        listener: RequestListener<Drawable>
    ) {
        loadImageWithResultListener.invoke(imageUrl, imageView, scaleType, thumbUrl, listener)
    }

    companion object {
        private lateinit var INSTANCE: ImageEditor

        val instance: ImageEditor get() = INSTANCE

        fun init(
            loadImageWithResultListener: ((String, ImageView, ScaleType, String, RequestListener<Drawable>) -> Unit)
        ) {
            INSTANCE = ImageEditor(loadImageWithResultListener)
        }
    }
}
