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
                        override fun onLoadFailed(e: Exception?, isFirstResource: Boolean) {
                            requestListener.onLoadFailed(e, isFirstResource)
                        }

                        override fun onResourceReady(resource: Drawable, isFirstResource: Boolean) {
                            requestListener.onResourceReady(resource, isFirstResource)
                        }
                    }
                )
            }
        }
    }
}
