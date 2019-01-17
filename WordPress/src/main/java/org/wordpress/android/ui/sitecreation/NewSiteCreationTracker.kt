package org.wordpress.android.ui.sitecreation

import org.wordpress.android.analytics.AnalyticsTracker
import javax.inject.Inject
import javax.inject.Singleton

const val ORIGIN_VERTICALS_FULLSCREEN_ERROR = "Verticals - fullscreen"
const val ORIGIN_VERTICALS_LIST_ITEM_ERROR = "Verticals - list item"
const val ORIGIN_SEGMENTS_ERROR = "Segments"
const val ORIGIN_DOMAINS_ERROR = "Domains"
const val ORIGIN_PREVIEW_ERROR = "Preview"

@Singleton
class NewSiteCreationTracker @Inject constructor() {
    fun trackSiteCreationAccessed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_ACCESSED)
    }

    fun trackSegmentsViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SEGMENTS_VIEWED)
    }

    fun trackSegmentSelected(segmentName: String, segmentId: Long) {
        AnalyticsTracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SEGMENTS_SELECTED,
                mapOf(
                        "segment_name" to segmentName,
                        "segment_id" to segmentId
                )
        )
    }

    fun trackVerticalsViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_VERTICALS_VIEWED)
    }

    fun trackVerticalSelected(verticalName: String, verticalId: String, isUserVertical: Boolean) {
        AnalyticsTracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_VERTICALS_SELECTED,
                mapOf(
                        "vertical_name" to verticalName,
                        "vertical_id" to verticalId,
                        "vertical_is_user" to isUserVertical
                )
        )
    }

    fun trackVerticalsSkipped() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_VERTICALS_SKIPPED)
    }

    fun trackBasicInformationViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_BASIC_INFORMATION_VIEWED)
    }

    fun trackBasicInformationCompleted(siteTitle: String, siteTagLine: String) {
        AnalyticsTracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_BASIC_INFORMATION_COMPLETED,
                mapOf(
                        "site_title" to siteTitle,
                        "tagline" to siteTagLine
                )
        )
    }

    fun trackBasicInformationSkipped() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_BASIC_INFORMATION_SKIPPED)
    }

    fun trackDomainsAccessed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_DOMAINS_ACCESSED)
    }

    fun trackDomainSelected(choosenDomain: String, searchTerm: String) {
        AnalyticsTracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_DOMAINS_SELECTED,
                mapOf(
                        "chosen_domain" to choosenDomain,
                        "search_term" to searchTerm
                )
        )
    }

    fun trackPreviewLoading() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SUCCESS_LOADING)
    }

    fun trackPreviewWebviewShown() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SUCCESS_PREVIEW_VIEWED)
    }

    fun trackPreviewWebviewFullyLoaded() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_SUCCESS_PREVIEW_LOADED)
    }

    fun trackCreationCompleted() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_COMPLETED)
    }

    fun trackConnectionErrorShown(origin: String) {
        AnalyticsTracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_ERROR_SHOWN,
                mapOf("error_info" to "connection error - $origin")
        )
    }

    fun trackGenericErrorShown(origin: String) {
        AnalyticsTracker.track(
                AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_ERROR_SHOWN,
                mapOf("error_info" to "generic error - $origin")
        )
    }

    fun trackSiteCreationServiceStateUpdated(props: Map<String, *>) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.ENHANCED_SITE_CREATION_BACKGROUND_SERVICE_UPDATED, props)
    }
}
