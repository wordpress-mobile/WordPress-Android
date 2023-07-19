package org.wordpress.android.ui.blaze.blazeCampaigns

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.ui.blaze.blazeCampaigns.campaigndetail.CampaignDetailPageSource
import org.wordpress.android.ui.blaze.blazeCampaigns.campaignlisting.CampaignListingPageSource

@Parcelize
@SuppressLint("ParcelCreator")
sealed class BlazeCampaignPage : Parcelable {
    data class CampaignListingPage(val source: CampaignListingPageSource) : BlazeCampaignPage()
    data class CampaignDetailsPage(val source: CampaignDetailPageSource) : BlazeCampaignPage()
    object Done: BlazeCampaignPage()
}
