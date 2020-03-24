package org.wordpress.android.imageeditor

import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import java.io.File

class ImageEditor private constructor(
    private val loadIntoImageViewWithResultListener: (
        (String, ImageView, ScaleType, String, RequestListener<Drawable>) -> Unit
    ),
    private val loadIntoFileWithResultListener: ((String, RequestListener<File>) -> Unit),
    private val trackData: ((Pair<TrackableEvent, TrackableAction?>) -> Unit)
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

    fun loadIntoImageViewWithResultListener(
        imageUrl: String,
        imageView: ImageView,
        scaleType: ScaleType,
        thumbUrl: String,
        listener: RequestListener<Drawable>
    ) {
        loadIntoImageViewWithResultListener.invoke(imageUrl, imageView, scaleType, thumbUrl, listener)
    }

    fun loadIntoFileWithResultListener(
        imageUrl: String,
        listener: RequestListener<File>
    ) {
        loadIntoFileWithResultListener.invoke(imageUrl, listener)
    }

    fun trackEventWithActionData(data: Pair<TrackableEvent, TrackableAction?>) {
        trackData(data)
    }

    enum class TrackableAction(val label: String) {
        CROP("crop")
    }

    enum class TrackableEvent {
        MEDIA_EDITOR_SHOWN,
        MEDIA_EDITOR_USED
    }

    companion object {
        private lateinit var INSTANCE: ImageEditor
        const val MEDIA_EDITOR_ACTIONS_KEY = "actions"

        val instance: ImageEditor get() = INSTANCE

        fun init(
            loadIntoImageViewWithResultListener: (
                (String, ImageView, ScaleType, String, RequestListener<Drawable>) -> Unit
            ),
            loadIntoFileWithResultListener: ((String, RequestListener<File>) -> Unit),
            trackData: ((Pair<TrackableEvent, TrackableAction?>) -> Unit)
        ) {
            INSTANCE = ImageEditor(
                loadIntoImageViewWithResultListener,
                loadIntoFileWithResultListener,
                trackData
            )
        }
    }
}
