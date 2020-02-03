package org.wordpress.android.imageeditor

import android.widget.ImageView
import java.util.concurrent.atomic.AtomicBoolean

class ImageEditor private constructor(
    private val loadImageUrlIntoImageView: ((String, ImageView) -> Unit)
) {
    companion object {
        private lateinit var INSTANCE: ImageEditor
        private val initialized = AtomicBoolean()

        val instance: ImageEditor get() = INSTANCE

        fun init(loadImageUrlIntoImageView: ((String, ImageView) -> Unit)) {
            if (initialized.getAndSet(true)) {
                INSTANCE = ImageEditor(loadImageUrlIntoImageView)
            }
        }
    }
}
