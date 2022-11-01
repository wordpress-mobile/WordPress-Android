package org.wordpress.android.ui.layoutpicker

interface ThumbDimensionProvider {
    val previewWidth: Int
    val previewHeight: Int
    val rowHeight: Int
    val scale: Double
        get() = 1.0 // Passing 1.0 and the rendered pixels per device in previewWidth
}
