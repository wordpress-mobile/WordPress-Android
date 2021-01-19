package org.wordpress.android.ui.sitecreation.theme

/**
 * Holds the available preview/thumbnail modes
 *
 * @param previewWidth the rendering width of the preview
 */
enum class PreviewMode(val previewWidth: Int) {
    MOBILE(400),
    TABLET(800),
    DESKTOP(1200)
}

/**
 * Defines an interface for handling the [PreviewMode]
 */
interface PreviewModeHandler {
    fun getPreviewMode(): PreviewMode
    fun setPreviewMode(mode: PreviewMode)
}
