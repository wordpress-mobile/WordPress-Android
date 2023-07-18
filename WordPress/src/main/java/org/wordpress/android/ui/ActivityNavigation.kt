package org.wordpress.android.ui

import android.content.Context
import android.content.Intent
import com.google.android.datatransport.runtime.dagger.Reusable
import org.wordpress.android.ui.blazeCampaigns.ARG_EXTRA_BLAZE_CAMPAIGN_PAGE
import org.wordpress.android.ui.blazeCampaigns.BlazeCampaignParentActivity
import org.wordpress.android.ui.blazeCampaigns.campaigndetail.CampaignDetailPageSource
import org.wordpress.android.ui.blazeCampaigns.campaignlisting.CampaignListingPageSource
import javax.inject.Inject

@Reusable
class ActivityNavigation @Inject constructor() {

    fun Context.navigateToCampaignListingPage(campaignListingPageSource: CampaignListingPageSource) {
        this.startActivity(
            Intent(this, BlazeCampaignParentActivity::class.java).apply {
                putExtra(ARG_EXTRA_BLAZE_CAMPAIGN_PAGE, campaignListingPageSource)
            }
        )
    }

    fun Context.navigateToCampaignDetailPage(campaignDetailPageSource: CampaignDetailPageSource) {
        this.startActivity(
            Intent(this, BlazeCampaignParentActivity::class.java).apply {
                putExtra(ARG_EXTRA_BLAZE_CAMPAIGN_PAGE, campaignDetailPageSource)
            }
        )
    }

}
