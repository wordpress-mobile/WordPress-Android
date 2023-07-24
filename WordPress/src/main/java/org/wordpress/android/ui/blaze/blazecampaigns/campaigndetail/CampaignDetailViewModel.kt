package org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import javax.inject.Inject

@HiltViewModel
class CampaignDetailViewModel @Inject constructor(
    private val blazeFeatureUtils: BlazeFeatureUtils
) : ViewModel() {
    private var campaignId: Int = 0
    fun start(campaignId: Int, campaignDetailPageSource: CampaignDetailPageSource) {
        blazeFeatureUtils.trackCampaignDetailsOpened(campaignDetailPageSource)
        this.campaignId = campaignId
    }
}

enum class CampaignDetailPageSource(val trackingName: String){
    DASHBOARD_CARD("dashboard_card"),
    CAMPAIGN_LISTING_PAGE("campaign_listing_page"),
    UNKNOWN("unknown")
}

