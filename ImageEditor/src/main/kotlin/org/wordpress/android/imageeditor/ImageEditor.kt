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
         * @param url The url of the image we were trying to load when the exception occurred.
         */
        fun onLoadFailed(e: Exception?, url: String)
        /**
         * Called when a load completes successfully
         *
         * @param resource The resource that was loaded for the target.
         * @param url The specific url that was used to load the image.
         */
        fun onResourceReady(resource: T, url: String)
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
