package org.wordpress.android.ui.posts.editor

import android.widget.ImageView.ScaleType.CENTER
import org.wordpress.android.imageeditor.ImageEditor
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.IMAGE

class ImageEditorInitializer {
    companion object {
        fun init(imageManager: ImageManager) {
            ImageEditor.init { imageUrl, imageView ->
                imageManager.load(imageView, IMAGE, imageUrl, CENTER)
            }
        }
    }
}
