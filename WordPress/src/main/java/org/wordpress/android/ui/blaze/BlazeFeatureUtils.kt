package org.wordpress.android.ui.blaze

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail.CampaignDetailPageSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingPageSource
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.BlazeFeatureConfig
import org.wordpress.android.util.config.BlazeManageCampaignFeatureConfig
import javax.inject.Inject

class BlazeFeatureUtils @Inject constructor(
    private val userAgent: UserAgent,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val blazeFeatureConfig: BlazeFeatureConfig,
    private val blazeManageCampaignFeatureConfig: BlazeManageCampaignFeatureConfig,
    private val buildConfigWrapper: BuildConfigWrapper,
) {
    private fun isBlazeEnabled(): Boolean {
        return buildConfigWrapper.isJetpackApp &&
                blazeFeatureConfig.isEnabled()
    }

    fun isPostBlazeEligible(
        siteModel: SiteModel,
        postStatus: PostStatus,
        postModel: PostModel
    ): Boolean {
        return isSiteBlazeEligible(siteModel) &&
                postStatus == PostStatus.PUBLISHED &&
                postModel.password.isEmpty()
    }

    fun isPageBlazeEligible(
        siteModel: SiteModel,
        pageStatus: PageStatus,
        pageModel: PageModel
    ): Boolean {
        return isSiteBlazeEligible(siteModel) &&
                pageStatus == PageStatus.PUBLISHED &&
                pageModel.post.password.isEmpty()
    }

    fun isSiteBlazeEligible(siteModel: SiteModel): Boolean {
        return siteModel.canBlaze != null &&
                siteModel.canBlaze &&
                siteModel.isAdmin &&
                isBlazeEnabled()
    }

    fun shouldShowBlazeCardEntryPoint(siteModel: SiteModel): Boolean =
        isSiteBlazeEligible(siteModel) &&
                !isBlazeCardHiddenByUser(siteModel.siteId)

    fun shouldShowBlazeCampaigns() = blazeManageCampaignFeatureConfig.isEnabled()

    fun shouldHideBlazeOverlay() = appPrefsWrapper.getShouldHideBlazeOverlay()

    fun setShouldHideBlazeOverlay() = appPrefsWrapper.setShouldHideBlazeOverlay(true)

    fun track(stat: AnalyticsTracker.Stat, source: BlazeFlowSource) {
        analyticsTrackerWrapper.track(
            stat,
            mapOf(SOURCE to source.trackingName)
        )
    }

    fun hideBlazeCard(siteId: Long) {
        appPrefsWrapper.setShouldHideBlazeCard(siteId, true)
    }

    fun trackEntryPointTapped(blazeFlowSource: BlazeFlowSource) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.BLAZE_ENTRY_POINT_TAPPED,
            mapOf(SOURCE to blazeFlowSource.trackingName)
        )
    }

    private fun isBlazeCardHiddenByUser(siteId: Long): Boolean {
        return appPrefsWrapper.hideBlazeCard(siteId)
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

    fun trackCampaignListingPageShown(campaignListingPageSource: CampaignListingPageSource) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.BLAZE_CAMPAIGN_LISTING_PAGE_SHOWN,
            mapOf(SOURCE to campaignListingPageSource.trackingName)
        )
    }

    fun trackCampaignDetailsOpened(campaignDetailPageSource: CampaignDetailPageSource) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.BLAZE_CAMPAIGN_DETAIL_PAGE_OPENED,
            mapOf(SOURCE to campaignDetailPageSource.trackingName)
        )
    }

    fun getUserAgent() = userAgent.toString()

    fun getAuthenticationPostData(authenticationUrl: String,
                                  urlToLoad: String,
                                  username: String,
                                  password: String,
                                  token: String): String =
        WPWebViewActivity.getAuthenticationPostData(authenticationUrl, urlToLoad, username, password, token)

    companion object {
        const val SOURCE = "source"
        const val CURRENT_STEP = "current_step"
    }
}
