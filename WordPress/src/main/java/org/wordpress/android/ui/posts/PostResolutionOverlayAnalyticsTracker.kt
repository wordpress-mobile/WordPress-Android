package org.wordpress.android.ui.posts

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class PostResolutionOverlayAnalyticsTracker @Inject constructor(
    private val tracker: AnalyticsTrackerWrapper
) {
    fun trackShown(postResolutionType: PostResolutionType, isPage: Boolean = false) {
        val stat = when (postResolutionType) {
            PostResolutionType.SYNC_CONFLICT -> AnalyticsTracker.Stat.RESOLVE_CONFLICT_SCREEN_SHOWN
            PostResolutionType.AUTOSAVE_REVISION_CONFLICT ->
                AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_SCREEN_SHOWN
        }

        tracker.track(stat, mapOf(
            PROPERTY_SOURCE to if (isPage) PROPERTY_SOURCE_PAGE else PROPERTY_SOURCE_POST)
        )
    }

    fun trackCancel(postResolutionType: PostResolutionType, isPage: Boolean = false) {
        val stat = when (postResolutionType) {
            PostResolutionType.SYNC_CONFLICT -> AnalyticsTracker.Stat.RESOLVE_CONFLICT_CANCEL_TAPPED
            PostResolutionType.AUTOSAVE_REVISION_CONFLICT ->
                AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_CANCEL_TAPPED
        }
        tracker.track(stat, mapOf(
            PROPERTY_SOURCE to if (isPage) PROPERTY_SOURCE_PAGE else PROPERTY_SOURCE_POST)
        )
    }

    fun trackClose(postResolutionType: PostResolutionType, isPage: Boolean = false) {
        val stat = when (postResolutionType) {
            PostResolutionType.SYNC_CONFLICT -> AnalyticsTracker.Stat.RESOLVE_CONFLICT_CLOSE_TAPPED
            PostResolutionType.AUTOSAVE_REVISION_CONFLICT ->
                AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_CLOSE_TAPPED
        }
        tracker.track(stat, mapOf(
            PROPERTY_SOURCE to if (isPage) PROPERTY_SOURCE_PAGE else PROPERTY_SOURCE_POST)
        )
    }

    fun trackDismissed(postResolutionType: PostResolutionType, isPage: Boolean = false) {
        val stat = when (postResolutionType) {
            PostResolutionType.SYNC_CONFLICT -> AnalyticsTracker.Stat.RESOLVE_CONFLICT_DISMISSED
            PostResolutionType.AUTOSAVE_REVISION_CONFLICT -> AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_DISMISSED
        }
        tracker.track(stat, mapOf(
            PROPERTY_SOURCE to if (isPage) PROPERTY_SOURCE_PAGE else PROPERTY_SOURCE_POST)
        )
    }

    fun trackConfirm(
        postResolutionType: PostResolutionType,
        confirmationType: PostResolutionConfirmationType,
        isPage: Boolean = false
    ) {
        val stat = when (postResolutionType) {
            PostResolutionType.SYNC_CONFLICT -> AnalyticsTracker.Stat.RESOLVE_CONFLICT_CONFIRM_TAPPED
            PostResolutionType.AUTOSAVE_REVISION_CONFLICT ->
                AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_CONFIRM_TAPPED
        }

        tracker.track(
            stat, mapOf(
                PROPERTY_CONFIRM_TYPE to confirmationType.analyticsLabel,
                PROPERTY_SOURCE to if (isPage) PROPERTY_SOURCE_PAGE else PROPERTY_SOURCE_POST
            )
        )
    }

    companion object {
        const val PROPERTY_CONFIRM_TYPE = "confirm_type"
        const val PROPERTY_SOURCE = "source"
        const val PROPERTY_SOURCE_PAGE = "page"
        const val PROPERTY_SOURCE_POST = "post"
    }
}
