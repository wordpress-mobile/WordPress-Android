package org.wordpress.android.ui.blaze

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeStatusModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.UriWrapper
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

    @Suppress("ReturnCount")
    fun extractCurrentStep(url: String?): BlazeFlowStep {
        url?.let {
            val uri = UriWrapper(url)
            uri.fragment?.let { return BlazeFlowStep.fromString(it) }

            if (findQueryParameter(uri.toString(), BLAZEPRESS_WIDGET) != null) {
                return BlazeFlowStep.STEP_1
            } else if (isAdvertisingCampaign(uri.toString())) {
                return BlazeFlowStep.CAMPAIGNS_LIST
            } else if (matchAdvertisingPath(uri.uri.path)) {
                return BlazeFlowStep.POSTS_LIST
            }
        }
        return BlazeFlowStep.UNSPECIFIED
    }

    private fun findQueryParameter(uri: String, parameterName: String): String? {
        val queryParams = uri.split("\\?".toRegex()).drop(1).joinToString("")
        val parameterRegex = "(^|&)${parameterName}=([^&]*)".toRegex()

        val parameterMatchResult = parameterRegex.find(queryParams)

        return parameterMatchResult?.groupValues?.getOrNull(2)
    }

    private fun isAdvertisingCampaign(uri: String): Boolean {
        val pattern = "https://wordpress.com/advertising/\\w+/campaigns$".toRegex()
        return pattern.matches(uri)
    }

    private fun matchAdvertisingPath(path: String?): Boolean {
        path?.let {
            val advertisingRegex = "^/advertising/[^/]+(/posts)?$".toRegex()
            return advertisingRegex.matches(it)
        }?: return false
    }

    companion object {
        const val SOURCE = "source"
        const val CURRENT_STEP = "current_step"
        const val BLAZEPRESS_WIDGET = "blazepress-widget"
    }
}
