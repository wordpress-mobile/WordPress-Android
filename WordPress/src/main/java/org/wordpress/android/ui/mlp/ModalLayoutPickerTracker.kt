package org.wordpress.android.ui.mlp

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.layoutpicker.LayoutPickerTracker
import org.wordpress.android.ui.mlp.ModalLayoutPickerTracker.PROPERTY.FILTER
import org.wordpress.android.ui.mlp.ModalLayoutPickerTracker.PROPERTY.LOCATION
import org.wordpress.android.ui.mlp.ModalLayoutPickerTracker.PROPERTY.PREVIEW_MODE
import org.wordpress.android.ui.mlp.ModalLayoutPickerTracker.PROPERTY.TEMPLATE
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Singleton

private const val LAYOUT_ERROR_CONTEXT = "layout"
private const val PAGE_PICKER_LOCATION = "page_picker"

@Singleton
class ModalLayoutPickerTracker @Inject constructor(val tracker: AnalyticsTrackerWrapper) : LayoutPickerTracker {
    private enum class PROPERTY(val key: String) {
        TEMPLATE("template"),
        PREVIEW_MODE("preview_mode"),
        LOCATION("location"),
        FILTER("filter")
    }

    override fun trackPreviewModeChanged(mode: String) {
        tracker.track(
                AnalyticsTracker.Stat.LAYOUT_PICKER_PREVIEW_MODE_CHANGED,
                mapOf(PREVIEW_MODE.key to mode)
        )
    }

    override fun trackThumbnailModeTapped(mode: String) {
        tracker.track(
                AnalyticsTracker.Stat.LAYOUT_PICKER_THUMBNAIL_MODE_BUTTON_TAPPED,
                mapOf(PREVIEW_MODE.key to mode)
        )
    }

    override fun trackPreviewModeTapped(mode: String) {
        tracker.track(
                AnalyticsTracker.Stat.LAYOUT_PICKER_PREVIEW_MODE_BUTTON_TAPPED,
                mapOf(PREVIEW_MODE.key to mode)
        )
    }

    override fun trackPreviewLoading(template: String, mode: String) {
        tracker.track(
                AnalyticsTracker.Stat.LAYOUT_PICKER_PREVIEW_LOADING,
                mapOf(TEMPLATE.key to template, PREVIEW_MODE.key to mode)
        )
    }

    override fun trackPreviewLoaded(template: String, mode: String) {
        tracker.track(
                AnalyticsTracker.Stat.LAYOUT_PICKER_PREVIEW_LOADED,
                mapOf(TEMPLATE.key to template, PREVIEW_MODE.key to mode)
        )
    }

    override fun trackPreviewViewed(template: String, mode: String) {
        tracker.track(
                AnalyticsTracker.Stat.LAYOUT_PICKER_PREVIEW_VIEWED,
                mapOf(TEMPLATE.key to template, PREVIEW_MODE.key to mode)
        )
    }

    override fun trackNoNetworkErrorShown(message: String) {
        tracker.track(
                AnalyticsTracker.Stat.LAYOUT_PICKER_ERROR_SHOWN,
                LAYOUT_ERROR_CONTEXT,
                "internet_unavailable_error",
                message
        )
    }

    override fun trackErrorShown(message: String) {
        tracker.track(
                AnalyticsTracker.Stat.LAYOUT_PICKER_ERROR_SHOWN,
                LAYOUT_ERROR_CONTEXT,
                "unknown",
                message
        )
    }

    override fun filterChanged(filter: List<String>) {
        tracker.track(
                AnalyticsTracker.Stat.FILTER_CHANGED,
                mapOf(LOCATION.key to PAGE_PICKER_LOCATION, FILTER.key to filter.joinToString())
        )
    }
}
