package org.wordpress.android.ui.posts.editor

import android.graphics.drawable.Drawable
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
                { imageUrl, imageView, scaleType, thumbUrl, requestListener ->
                    imageManager.loadWithResultListener(
                        imageView,
                        IMAGE,
                        imageUrl,
                        scaleType,
                        thumbUrl,
                        object : RequestListener<Drawable> {
                            override fun onLoadFailed(e: Exception?, model: Any?) {
                                if (model != null && model is String) {
                                    requestListener.onLoadFailed(e, model)
                                } else {
                                    throw(IllegalArgumentException(IMAGE_STRING_URL_MSG))
                                }
                            }

                            override fun onResourceReady(resource: Drawable, model: Any?) {
                                if (model != null && model is String) {
                                    requestListener.onResourceReady(resource, model)
                                } else {
                                    throw(IllegalArgumentException(IMAGE_STRING_URL_MSG))
                                }
                            }
                        }
                    )
                }

        private fun loadIntoFileWithResultListener(
            imageManager: ImageManager
        ): (String, ImageEditor.RequestListener<File>) -> Unit = { imageUrl, requestListener ->
            imageManager.loadIntoFileWithResultListener(
                imageUrl,
                object : RequestListener<File> {
                    override fun onLoadFailed(e: Exception?, model: Any?) {
                        if (model != null && model is String) {
                            requestListener.onLoadFailed(e, model)
                        } else {
                            throw(IllegalArgumentException(IMAGE_STRING_URL_MSG))
                        }
                    }

                    override fun onResourceReady(resource: File, model: Any?) {
                        if (model != null && model is String) {
                            requestListener.onResourceReady(resource, model)
                        } else {
                            throw(IllegalArgumentException(IMAGE_STRING_URL_MSG))
                        }
                    }
                }
            )
        }
    }
}
