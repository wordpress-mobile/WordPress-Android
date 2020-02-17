package org.wordpress.android.ui.posts.editor

import android.graphics.drawable.Drawable
import org.wordpress.android.imageeditor.ImageEditor
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageManager.RequestListener
import org.wordpress.android.util.image.ImageType.IMAGE

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
                            requestListener.onLoadFailed(e, model)
                        }

                        override fun onResourceReady(resource: Drawable, model: Any?) {
                            requestListener.onResourceReady(resource, model)
                        }
                    }
                )
            }
        }
    }
}
