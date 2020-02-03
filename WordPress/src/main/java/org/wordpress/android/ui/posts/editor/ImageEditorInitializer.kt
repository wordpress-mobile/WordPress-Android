package org.wordpress.android.ui.posts.editor

import org.wordpress.android.imageeditor.ImageEditor
import org.wordpress.android.util.image.ImageManager

class ImageEditorInitializer {
    companion object {
        fun init(imageManager: ImageManager) {
            ImageEditor.dummyImageEditorSingleton = ImageEditor { url ->
                imageManager.loadUrlIntoFile(url)
            }
        }
    }
}
