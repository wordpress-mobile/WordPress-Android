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
    fun shouldShowPromoteWithBlaze(
        postStatus: PostStatus,
        siteModel: SiteModel,
        postModel: PostModel
    ): Boolean {
        // add the logic to check whether the site is eligible for blaze
        return buildConfigWrapper.isJetpackApp &&
                blazeFeatureConfig.isEnabled() &&
                postStatus == PostStatus.PUBLISHED &&
                postModel.password.isEmpty() &&
                siteModel.isAdmin
    }

    fun shouldShowPromoteWithBlazeCard(blazeStatusModel: BlazeStatusModel?): Boolean {
        val isEligible = blazeStatusModel?.isEligible == true
        return buildConfigWrapper.isJetpackApp &&
                blazeFeatureConfig.isEnabled() &&
                isEligible &&
                !isPromoteWithBlazeCardHiddenByUser()
    }

    fun track(stat: AnalyticsTracker.Stat, source: BlazeFlowSource) {
        analyticsTrackerWrapper.track(
            stat,
            mapOf(SOURCE to source.trackingName)
        )
    }

    fun hidePromoteWithBlazeCard() {
        appPrefsWrapper.setShouldHidePromoteWithBlazeCard(true)
    }

    private fun isPromoteWithBlazeCardHiddenByUser(): Boolean {
        return appPrefsWrapper.getShouldHidePromoteWithBlazeCard()
    }

    companion object {
        const val SOURCE = "source"
    }

    @Suppress("UNUSED_PARAMETER")
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
        //todo: add tracking logic
    }
}
