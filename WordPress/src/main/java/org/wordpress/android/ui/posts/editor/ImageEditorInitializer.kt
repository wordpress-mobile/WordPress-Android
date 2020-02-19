package org.wordpress.android.ui.posts.editor

import android.graphics.drawable.Drawable
import org.wordpress.android.imageeditor.ImageEditor
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageManager.RequestListener
import org.wordpress.android.util.image.ImageType.IMAGE
import java.lang.IllegalArgumentException

class ImageEditorInitializer {
    companion object {
        fun init(imageManager: ImageManager) {
            ImageEditor.init { imageUrl, imageView, scaleType, thumbUrl, requestListener ->
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
                                throw(IllegalArgumentException("ImageEditor requires a not-null string image url."))
                            }
                        }

                        override fun onResourceReady(resource: Drawable, model: Any?) {
                            if (model != null && model is String) {
                                requestListener.onResourceReady(resource, model)
                            } else {
                                throw(IllegalArgumentException("ImageEditor requires a not-null string image url."))
                            }
                        }
                    }
                )
            }
        }
    }
}
