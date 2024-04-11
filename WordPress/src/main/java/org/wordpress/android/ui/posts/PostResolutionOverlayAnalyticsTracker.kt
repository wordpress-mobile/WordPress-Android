package org.wordpress.android.ui.posts

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class PostResolutionOverlayAnalyticsTracker @Inject constructor(
    private val tracker: AnalyticsTrackerWrapper
) {
    fun trackShown(postResolutionType: PostResolutionType) {
        val stat = when (postResolutionType) {
            PostResolutionType.SYNC_CONFLICT -> AnalyticsTracker.Stat.RESOLVE_CONFLICT_SCREEN_SHOWN
            PostResolutionType.AUTOSAVE_REVISION_CONFLICT ->
                AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_SCREEN_SHOWN
        }
        tracker.track(stat)
    }

    fun trackCancel(postResolutionType: PostResolutionType) {
        val stat = when (postResolutionType) {
            PostResolutionType.SYNC_CONFLICT -> AnalyticsTracker.Stat.RESOLVE_CONFLICT_CANCEL_TAPPED
            PostResolutionType.AUTOSAVE_REVISION_CONFLICT ->
                AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_CANCEL_TAPPED
        }
        tracker.track(stat)
    }

    fun trackClose(postResolutionType: PostResolutionType) {
        val stat = when (postResolutionType) {
            PostResolutionType.SYNC_CONFLICT -> AnalyticsTracker.Stat.RESOLVE_CONFLICT_CLOSE_TAPPED
            PostResolutionType.AUTOSAVE_REVISION_CONFLICT ->
                AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_CLOSE_TAPPED
        }
        tracker.track(stat)
    }

    fun trackDismissed(postResolutionType: PostResolutionType) {
        val stat = when (postResolutionType) {
            PostResolutionType.SYNC_CONFLICT -> AnalyticsTracker.Stat.RESOLVE_CONFLICT_DISMISSED
            PostResolutionType.AUTOSAVE_REVISION_CONFLICT -> AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_DISMISSED
        }
        tracker.track(stat)
    }

    fun trackConfirm(postResolutionType: PostResolutionType, confirmationType: PostResolutionConfirmationType) {
        val stat = when (postResolutionType) {
            PostResolutionType.SYNC_CONFLICT -> AnalyticsTracker.Stat.RESOLVE_CONFLICT_CONFIRM_TAPPED
            PostResolutionType.AUTOSAVE_REVISION_CONFLICT ->
                AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_CONFIRM_TAPPED
        }
        tracker.track(stat, mapOf(PROPERTY_CONFIRM_TYPE to confirmationType.analyticsLabel))
    }

    companion object {
        const val PROPERTY_CONFIRM_TYPE = "confirm_type"
    }
}
