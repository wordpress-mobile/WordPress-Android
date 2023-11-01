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
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainActivity
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainActivity.Companion.DOMAIN_CANDIDATE_ID_KEY
import org.wordpress.android.ui.mysite.menu.KEY_QUICK_START_EVENT
import org.wordpress.android.ui.mysite.menu.MenuActivity
import org.wordpress.android.ui.mysite.personalization.PersonalizationActivity
import org.wordpress.android.ui.quickstart.QuickStartEvent
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

    fun openDashboardPersonalization(context: Context) {
        context.startActivity(Intent(context, PersonalizationActivity::class.java))
    }

    fun openUnifiedMySiteMenu(context: Context, quickStartEvent: QuickStartEvent? = null) {
        if (quickStartEvent != null) {
            context.startActivity(
                Intent(context, MenuActivity::class.java).apply {
                    putExtra(KEY_QUICK_START_EVENT, quickStartEvent)
                }
            )
            return
        }
        context.startActivity(Intent(context, MenuActivity::class.java))
    }

    fun openPurchaseDomain(context: Context, domainId: Int) {
        context.startActivity(
            Intent(context, PurchaseDomainActivity::class.java).apply {
                putExtra(DOMAIN_CANDIDATE_ID_KEY, domainId)
            }
        )
    }
}
