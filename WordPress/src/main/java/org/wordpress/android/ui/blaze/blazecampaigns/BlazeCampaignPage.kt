package org.wordpress.android.ui.blaze.blazecampaigns

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail.CampaignDetailPageSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingPageSource

@Parcelize
@SuppressLint("ParcelCreator")
sealed class BlazeCampaignPage : Parcelable {
    data class CampaignListingPage(val source: CampaignListingPageSource) : BlazeCampaignPage()
    data class CampaignDetailsPage(val campaignId: String, val source: CampaignDetailPageSource) : BlazeCampaignPage()
    object Done: BlazeCampaignPage()
}
