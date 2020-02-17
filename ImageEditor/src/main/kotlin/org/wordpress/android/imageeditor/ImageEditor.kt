package org.wordpress.android.imageeditor

import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.ImageView.ScaleType

class ImageEditor private constructor(
    private val loadImageWithResultListener: ((String, ImageView, ScaleType, String, RequestListener<Drawable>) -> Unit)

) {
    interface RequestListener<T> {
        /**
         * Called when an exception occurs during an image load
         *
         * @param e The maybe {@code null} exception containing information about why the request failed.
         * @param model The model we were trying to load when the exception occurred.
         */
        fun onLoadFailed(e: Exception?, model: Any?)
        /**
         * Called when a load completes successfully
         *
         * @param resource The resource that was loaded for the target.
         * @param model The specific model that was used to load the image.
         */
        fun onResourceReady(resource: T, model: Any?)
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
