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

    fun trackPreviewLoading() {
        tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SUCCESS_LOADING)
    }

    fun trackPreviewWebviewShown() {
        tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SUCCESS_PREVIEW_VIEWED)
    }

    fun trackPreviewWebviewFullyLoaded() {
        tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SUCCESS_PREVIEW_LOADED)
    }

    fun trackPreviewOkButtonTapped() {
        tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_PREVIEW_OK_BUTTON_TAPPED)
    }

    /**
     * This stat is part of a funnel that provides critical information.  Before
     * making ANY modification to this stat please refer to: p4qSXL-35X-p2
     */
    fun trackSiteCreated() {
        tracker.track(AnalyticsTracker.Stat.SITE_CREATED)
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
}
