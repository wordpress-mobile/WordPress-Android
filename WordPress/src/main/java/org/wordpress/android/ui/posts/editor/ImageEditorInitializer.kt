package org.wordpress.android.ui.posts.editor

import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import org.wordpress.android.imageeditor.ImageEditor
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageManager.RequestListener
import org.wordpress.android.util.image.ImageType.IMAGE
import java.io.File

class ImageEditorInitializer {
    companion object {
        private const val IMAGE_STRING_URL_MSG = "ImageEditor requires a not-null string image url."

        fun init(imageManager: ImageManager) {
            ImageEditor.init(
                loadIntoImageViewWithResultListener(imageManager),
                loadIntoFileWithResultListener(imageManager)
            )
        }

        private fun loadIntoImageViewWithResultListener(
            imageManager: ImageManager
        ): (String, ImageView, ScaleType, String, ImageEditor.RequestListener<Drawable>) -> Unit =
                { imageUrl, imageView, scaleType, thumbUrl, listener ->
                    imageManager.loadWithResultListener(
                        imageView,
                        IMAGE,
                        imageUrl,
                        scaleType,
                        thumbUrl,
                        object : RequestListener<Drawable> {
                            override fun onLoadFailed(e: Exception?, model: Any?) = onLoadFailed(model, listener, e)
                            override fun onResourceReady(resource: Drawable, model: Any?) =
                                onResourceReady(model, listener, resource)
                        }
                    )
                }

        private fun loadIntoFileWithResultListener(imageManager: ImageManager):
                (String, ImageEditor.RequestListener<File>) -> Unit = { imageUrl, listener ->
            imageManager.loadIntoFileWithResultListener(
                imageUrl,
                object : RequestListener<File> {
                    override fun onLoadFailed(e: Exception?, model: Any?) = onLoadFailed(model, listener, e)
                    override fun onResourceReady(resource: File, model: Any?) =
                        onResourceReady(model, listener, resource)
                }
            )
        }

        private fun <T : Any> onResourceReady(model: Any?, listener: ImageEditor.RequestListener<T>, resource: T) =
            if (model != null && (model is String || model is Uri)) {
                listener.onResourceReady(resource, model.toString())
            } else {
                throw(IllegalArgumentException(IMAGE_STRING_URL_MSG))
            }

        private fun <T : Any> onLoadFailed(model: Any?, listener: ImageEditor.RequestListener<T>, e: Exception?) =
            if (model != null && (model is String || model is Uri)) {
                listener.onLoadFailed(e, model.toString())
            } else {
                throw(IllegalArgumentException(IMAGE_STRING_URL_MSG))
            }
    }
}
