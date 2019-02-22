package org.wordpress.android.ui.sitecreation.misc

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Singleton

enum class NewSiteCreationErrorType {
    INTERNET_UNAVAILABLE_ERROR,
    UNKNOWN;
}

@Singleton
class NewSiteCreationTracker @Inject constructor(val tracker: AnalyticsTrackerWrapper) {
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

    fun trackVerticalsViewed() {
        tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_VERTICALS_VIEWED)
    }

    fun trackVerticalSelected(verticalName: String, verticalId: String, isUserVertical: Boolean) {
        tracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_VERTICALS_SELECTED,
                mapOf(
                        "vertical_name" to verticalName,
                        "vertical_id" to verticalId,
                        "vertical_is_user" to isUserVertical
                )
        )
    }

    fun trackVerticalsSkipped() {
        tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_VERTICALS_SKIPPED)
    }

    fun trackBasicInformationViewed() {
        tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_BASIC_INFORMATION_VIEWED)
    }

    fun trackBasicInformationCompleted(siteTitle: String, siteTagLine: String) {
        tracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_BASIC_INFORMATION_COMPLETED,
                mapOf(
                        "site_title" to siteTitle,
                        "tagline" to siteTagLine
                )
        )
    }

    fun trackBasicInformationSkipped() {
        tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_BASIC_INFORMATION_SKIPPED)
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

    fun trackCreationCompleted() {
        tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_COMPLETED)
    }

    fun trackFlowExited() {
        tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_EXITED)
    }

    fun trackErrorShown(errorContext: String, errorType: NewSiteCreationErrorType, errorDescription: String? = null) {
        trackErrorShown(errorContext, errorType.toString().toLowerCase(), errorDescription)
    }

    fun trackErrorShown(errorContext: String, errorType: String, errorDescription: String? = null) {
        tracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_ERROR_SHOWN,
                errorContext,
                errorType.toLowerCase(),
                errorDescription ?: ""
        )
    }

    fun trackSiteCreationServiceStateUpdated(props: Map<String, *>) {
        tracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_BACKGROUND_SERVICE_UPDATED, props)
    }
}
