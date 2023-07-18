package org.wordpress.android.ui.blazeCampaigns

import org.wordpress.android.ui.blazeCampaigns.campaigndetail.CampaignDetailPageSource
import org.wordpress.android.ui.blazeCampaigns.campaignlisting.CampaignListingPageSource

sealed class BlazeCampaignPage {
    data class CampaignListingPage(val source: CampaignListingPageSource) : BlazeCampaignPage()
    data class CampaignDetailsPage(val source: CampaignDetailPageSource) : BlazeCampaignPage()
    object Done: BlazeCampaignPage()
}
