package org.wordpress.android.ui.blaze

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeStatusModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.BlazeFeatureConfig
import javax.inject.Inject

class BlazeFeatureUtils @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val blazeFeatureConfig: BlazeFeatureConfig,
    private val buildConfigWrapper: BuildConfigWrapper,
) {
    fun isBlazeEnabled(): Boolean {
        return buildConfigWrapper.isJetpackApp &&
                blazeFeatureConfig.isEnabled()
    }

    fun isBlazeEligibleForUser(siteModel: SiteModel): Boolean {
        return siteModel.isAdmin &&
                isBlazeEnabled()
    }

    fun isPostBlazeEligible(
        postStatus: PostStatus,
        postModel: PostModel
    ): Boolean {
        return isBlazeEnabled() &&
                postStatus == PostStatus.PUBLISHED &&
                postModel.password.isEmpty()
    }

    fun shouldShowBlazeCardEntryPoint(blazeStatusModel: BlazeStatusModel?, siteId: Long) =
        isBlazeEnabled() &&
                blazeStatusModel?.isEligible == true &&
                    !isPromoteWithBlazeCardHiddenByUser(siteId)

    fun shouldShowBlazeMenuEntryPoint(blazeStatusModel: BlazeStatusModel?) =
        isBlazeEnabled() &&  blazeStatusModel?.isEligible == true

    fun track(stat: AnalyticsTracker.Stat, source: BlazeFlowSource) {
        analyticsTrackerWrapper.track(
            stat,
            mapOf(SOURCE to source.trackingName)
        )
    }

    fun hidePromoteWithBlazeCard(siteId: Long) {
        appPrefsWrapper.setShouldHidePromoteWithBlazeCard(siteId,true)
    }

    fun trackEntryPointTapped(blazeFlowSource: BlazeFlowSource) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.BLAZE_ENTRY_POINT_TAPPED,
            mapOf(SOURCE to blazeFlowSource.trackingName)
        )
    }

    private fun isPromoteWithBlazeCardHiddenByUser(siteId: Long): Boolean {
        return appPrefsWrapper.getShouldHidePromoteWithBlazeCard(siteId)
    }

    fun trackOverlayDisplayed(blazeFlowSource: BlazeFlowSource) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.BLAZE_FEATURE_OVERLAY_DISPLAYED,
            mapOf(SOURCE to blazeFlowSource.trackingName)
        )
    }

    fun trackPromoteWithBlazeClicked(blazeFlowSource: BlazeFlowSource) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.BLAZE_FEATURE_OVERLAY_PROMOTE_CLICKED,
            mapOf(SOURCE to blazeFlowSource.trackingName)
        )
    }

    fun trackOverlayDismissed(blazeFlowSource: BlazeFlowSource) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.BLAZE_FEATURE_OVERLAY_DISMISSED,
            mapOf(SOURCE to blazeFlowSource.trackingName)
        )
    }

    fun trackFlowError(blazeFlowSource: BlazeFlowSource, blazeFlowStep: BlazeFlowStep) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.BLAZE_FLOW_ERROR,
            mapOf(
                SOURCE to blazeFlowSource.trackingName, CURRENT_STEP to blazeFlowStep.trackingName
            )
        )
    }

    fun trackFlowCanceled(blazeFlowSource: BlazeFlowSource, blazeFlowStep: BlazeFlowStep) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.BLAZE_FLOW_CANCELED,
            mapOf(
                SOURCE to blazeFlowSource.trackingName, CURRENT_STEP to blazeFlowStep.trackingName
            )
        )
    }

    fun trackBlazeFlowStarted(blazeFlowSource: BlazeFlowSource) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.BLAZE_FLOW_STARTED, mapOf(SOURCE to blazeFlowSource.trackingName)
        )
    }

    fun trackBlazeFlowCompleted(blazeFlowSource: BlazeFlowSource) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.BLAZE_FLOW_COMPLETED, mapOf(SOURCE to blazeFlowSource.trackingName)
        )
    }

    companion object {
        const val SOURCE = "source"
        const val CURRENT_STEP = "current_step"
    }
}
