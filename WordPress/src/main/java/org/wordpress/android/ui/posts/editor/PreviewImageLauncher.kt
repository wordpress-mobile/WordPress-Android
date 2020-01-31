package org.wordpress.android.ui.posts.editor

import android.app.Activity
import org.wordpress.android.imageeditor.ImageEditor
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.util.image.ImageManager
import java.io.Serializable

class PreviewImageLauncher(
    private val imageManager: ImageManager
) : Serializable {
    private val editor: ImageEditor

    init {
        editor = ImageEditor { url ->
            imageManager.loadUrlIntoFile(url)
        }
    }

    fun start(activity: Activity) {
        ActivityLauncher.openImageEditor(activity, editor)
    }
}
