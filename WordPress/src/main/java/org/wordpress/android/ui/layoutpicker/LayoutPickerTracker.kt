package org.wordpress.android.ui.layoutpicker

interface LayoutPickerTracker {
    fun trackPreviewModeChanged(mode: String)

    fun trackThumbnailModeTapped(mode: String)

    fun trackPreviewModeTapped(mode: String)

    fun trackPreviewLoading(template: String, mode: String)

    fun trackPreviewLoaded(template: String, mode: String)

    fun trackPreviewViewed(template: String, mode: String)

    fun trackNoNetworkErrorShown(message: String)

    fun trackErrorShown(message: String)
}
