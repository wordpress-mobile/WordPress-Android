package org.wordpress.android.ui.sitecreation.misc

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.layoutpicker.LayoutPickerTracker
import org.wordpress.android.ui.sitecreation.misc.SiteCreationErrorType.INTERNET_UNAVAILABLE_ERROR
import org.wordpress.android.ui.sitecreation.misc.SiteCreationErrorType.UNKNOWN
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker.PROPERTY.CHOSEN_DOMAIN
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker.PROPERTY.PREVIEW_MODE
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker.PROPERTY.SEARCH_TERM
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker.PROPERTY.SEGMENT_ID
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker.PROPERTY.SEGMENT_NAME
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker.PROPERTY.TEMPLATE
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker.PROPERTY.THUMBNAIL_MODE
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class SiteCreationErrorType {
    INTERNET_UNAVAILABLE_ERROR,
    UNKNOWN;
}

private const val DESIGN_ERROR_CONTEXT = "design"

@Singleton
class SiteCreationTracker @Inject constructor(val tracker: AnalyticsTrackerWrapper) : LayoutPickerTracker {
    private enum class PROPERTY(val key: String) {
        TEMPLATE("template"),
        SEGMENT_NAME("segment_name"),
        SEGMENT_ID("segment_id"),
        CHOSEN_DOMAIN("chosen_domain"),
        SEARCH_TERM("search_term"),
        THUMBNAIL_MODE("thumbnail_mode"),
        PREVIEW_MODE("preview_mode")
    }

    private var designSelectionSkipped: Boolean = false

    fun trackSiteCreationAccessed() {
        tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_ACCESSED)
    }

    fun trackSegmentsViewed() {
        tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SEGMENTS_VIEWED)
    }

    fun trackSegmentSelected(segmentName: String, segmentId: Long) {
        tracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SEGMENTS_SELECTED,
                mapOf(
                        SEGMENT_NAME.key to segmentName,
                        SEGMENT_ID.key to segmentId
                )
        )
    }

    fun trackDomainsAccessed() {
        tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_DOMAINS_ACCESSED)
    }

    fun trackDomainSelected(chosenDomain: String, searchTerm: String) {
        tracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_DOMAINS_SELECTED,
                mapOf(
                        CHOSEN_DOMAIN.key to chosenDomain,
                        SEARCH_TERM.key to searchTerm
                )
        )
    }

    fun trackPreviewLoading(template: String?) {
        if (template == null || designSelectionSkipped) {
            tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SUCCESS_LOADING)
        } else {
            tracker.track(
                    AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SUCCESS_LOADING,
                    mapOf(TEMPLATE.key to template)
            )
        }
    }

    fun trackPreviewWebviewShown(template: String?) {
        if (template == null || designSelectionSkipped) {
            tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SUCCESS_PREVIEW_VIEWED)
        } else {
            tracker.track(
                    AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SUCCESS_PREVIEW_VIEWED,
                    mapOf(TEMPLATE.key to template)
            )
        }
    }

    fun trackPreviewWebviewFullyLoaded(template: String?) {
        if (template == null || designSelectionSkipped) {
            tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SUCCESS_PREVIEW_LOADED)
        } else {
            tracker.track(
                    AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SUCCESS_PREVIEW_LOADED,
                    mapOf(TEMPLATE.key to template)
            )
        }
    }

    fun trackPreviewOkButtonTapped() {
        tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_PREVIEW_OK_BUTTON_TAPPED)
    }

    /**
     * This stat is part of a funnel that provides critical information.  Before
     * making ANY modification to this stat please refer to: p4qSXL-35X-p2
     */
    fun trackSiteCreated(template: String?) {
        if (template == null || designSelectionSkipped) {
            tracker.track(AnalyticsTracker.Stat.SITE_CREATED)
        } else {
            tracker.track(AnalyticsTracker.Stat.SITE_CREATED, mapOf(TEMPLATE.key to template))
        }
    }

    fun trackFlowExited() {
        tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_EXITED)
    }

    fun trackErrorShown(errorContext: String, errorType: SiteCreationErrorType, errorDescription: String? = null) {
        trackErrorShown(errorContext, errorType.toString().toLowerCase(Locale.ROOT), errorDescription)
    }

    fun trackErrorShown(errorContext: String, errorType: String, errorDescription: String? = null) {
        tracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_ERROR_SHOWN,
                errorContext,
                errorType.toLowerCase(Locale.ROOT),
                errorDescription ?: ""
        )
    }

    fun trackSiteCreationServiceStateUpdated(props: Map<String, *>) {
        tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_BACKGROUND_SERVICE_UPDATED, props)
    }

    fun trackSiteDesignViewed(previewMode: String) {
        tracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SITE_DESIGN_VIEWED,
                mapOf(THUMBNAIL_MODE.key to previewMode)
        )
    }

    override fun trackThumbnailModeTapped(mode: String) {
        tracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SITE_DESIGN_THUMBNAIL_MODE_BUTTON_TAPPED,
                mapOf(PREVIEW_MODE.key to mode)
        )
    }

    fun trackSiteDesignSkipped() {
        designSelectionSkipped = true
        tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SITE_DESIGN_SKIPPED)
    }

    fun trackSiteDesignSelected(template: String) {
        designSelectionSkipped = false
        tracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SITE_DESIGN_SELECTED,
                mapOf(TEMPLATE.key to template)
        )
    }

    override fun trackPreviewViewed(template: String, mode: String) {
        tracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SITE_DESIGN_PREVIEW_VIEWED,
                mapOf(TEMPLATE.key to template, PREVIEW_MODE.key to mode)
        )
    }

    override fun trackPreviewLoading(template: String, mode: String) {
        tracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SITE_DESIGN_PREVIEW_LOADING,
                mapOf(TEMPLATE.key to template, PREVIEW_MODE.key to mode)
        )
    }

    override fun trackPreviewLoaded(template: String, mode: String) {
        tracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SITE_DESIGN_PREVIEW_LOADED,
                mapOf(TEMPLATE.key to template, PREVIEW_MODE.key to mode)
        )
    }

    override fun trackPreviewModeTapped(mode: String) {
        tracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SITE_DESIGN_PREVIEW_MODE_BUTTON_TAPPED,
                mapOf(PREVIEW_MODE.key to mode)
        )
    }

    override fun trackPreviewModeChanged(mode: String) {
        tracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SITE_DESIGN_PREVIEW_MODE_CHANGED,
                mapOf(PREVIEW_MODE.key to mode)
        )
    }

    override fun trackNoNetworkErrorShown(message: String) =
            trackErrorShown(DESIGN_ERROR_CONTEXT, INTERNET_UNAVAILABLE_ERROR, message)

    override fun trackErrorShown(message: String) = trackErrorShown(DESIGN_ERROR_CONTEXT, UNKNOWN, message)
}
