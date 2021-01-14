package org.wordpress.android.ui.sitecreation.theme

/**
 * Holds the available preview/thumbnail modes
 */
enum class PreviewMode {
    MOBILE,
    TABLET,
    DESKTOP
}

/**
 * Defines an interface for handling the [PreviewMode]
 */
interface PreviewModeHandler {
    fun getPreviewMode(): PreviewMode
    fun setPreviewMode(mode: PreviewMode)
}
