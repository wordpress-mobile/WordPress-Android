package org.wordpress.android.ui.sitecreation.misc

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class SiteCreationErrorType {
    INTERNET_UNAVAILABLE_ERROR,
    UNKNOWN;
}

@Singleton
class SiteCreationTracker @Inject constructor(val tracker: AnalyticsTrackerWrapper) {
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
                        "segment_name" to segmentName,
                        "segment_id" to segmentId
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
                        "chosen_domain" to chosenDomain,
                        "search_term" to searchTerm
                )
        )
    }

    fun trackPreviewLoading(template: String?) {
        if (template == null || designSelectionSkipped) {
            tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SUCCESS_LOADING)
        } else {
            tracker.track(
                    AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SUCCESS_LOADING,
                    mapOf("template" to template)
            )
        }
    }

    fun trackPreviewWebviewShown(template: String?) {
        if (template == null || designSelectionSkipped) {
            tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SUCCESS_PREVIEW_VIEWED)
        } else {
            tracker.track(
                    AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SUCCESS_PREVIEW_VIEWED,
                    mapOf("template" to template)
            )
        }
    }

    fun trackPreviewWebviewFullyLoaded(template: String?) {
        if (template == null || designSelectionSkipped) {
            tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SUCCESS_PREVIEW_LOADED)
        } else {
            tracker.track(
                    AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SUCCESS_PREVIEW_LOADED,
                    mapOf("template" to template)
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
            tracker.track(AnalyticsTracker.Stat.SITE_CREATED, mapOf("template" to template))
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

    fun trackSiteDesignViewed() {
        tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SITE_DESIGN_VIEWED)
    }

    fun trackSiteDesignSkipped() {
        designSelectionSkipped = true
        tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SITE_DESIGN_SKIPPED)
    }

    fun trackSiteDesignSelected(template: String) {
        designSelectionSkipped = false
        tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SITE_DESIGN_SELECTED, mapOf("template" to template))
    }

    fun trackSiteDesignPreviewViewed(template: String) {
        tracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SITE_DESIGN_PREVIEW_VIEWED,
                mapOf("template" to template)
        )
    }

    fun trackSiteDesignPreviewLoading(template: String) {
        tracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SITE_DESIGN_PREVIEW_LOADING,
                mapOf("template" to template)
        )
    }

    fun trackSiteDesignPreviewLoaded(template: String) {
        tracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SITE_DESIGN_PREVIEW_LOADED,
                mapOf("template" to template)
        )
    }
}
