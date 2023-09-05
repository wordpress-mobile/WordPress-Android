package org.wordpress.android.ui

import android.content.Context
import android.content.Intent
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.blaze.blazecampaigns.ARG_EXTRA_BLAZE_CAMPAIGN_PAGE
import org.wordpress.android.ui.blaze.blazecampaigns.BlazeCampaignPage
import org.wordpress.android.ui.blaze.blazecampaigns.BlazeCampaignParentActivity
import org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail.CampaignDetailPageSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingPageSource
import org.wordpress.android.ui.blaze.blazepromote.ARG_BLAZE_FLOW_SOURCE
import org.wordpress.android.ui.blaze.blazepromote.ARG_BLAZE_SHOULD_SHOW_OVERLAY
import org.wordpress.android.ui.blaze.blazepromote.BlazePromoteParentActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityNavigator @Inject constructor() {
    fun navigateToCampaignListingPage(context: Context, campaignListingPageSource: CampaignListingPageSource) {
        context.startActivity(
            Intent(context, BlazeCampaignParentActivity::class.java).apply {
                putExtra(
                    ARG_EXTRA_BLAZE_CAMPAIGN_PAGE,
                    BlazeCampaignPage.CampaignListingPage(campaignListingPageSource)
                )
            }
        )
    }

    fun navigateToCampaignDetailPage(
        context: Context,
        campaignId: Int,
        campaignDetailPageSource: CampaignDetailPageSource
    ) {
        context.startActivity(
            Intent(context, BlazeCampaignParentActivity::class.java).apply {
                putExtra(
                    ARG_EXTRA_BLAZE_CAMPAIGN_PAGE,
                    BlazeCampaignPage.CampaignDetailsPage(campaignId, campaignDetailPageSource)
                )
            }
        )
    }

    fun openPromoteWithBlaze(
        context: Context,
        source: BlazeFlowSource,
        shouldShowOverlay: Boolean = false
    ) {
        val intent = Intent(context, BlazePromoteParentActivity::class.java)
        intent.putExtra(ARG_BLAZE_FLOW_SOURCE, source)
        intent.putExtra(ARG_BLAZE_SHOULD_SHOW_OVERLAY, shouldShowOverlay)
        context.startActivity(intent)
    }

    fun openDomainTransfer(
        context: Context,
        url: String
    ) {
        WPWebViewActivity.openUrlByUsingGlobalWPCOMCredentials(context, url)
    }
}
