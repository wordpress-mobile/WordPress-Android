package org.wordpress.android.ui.blazeCampaigns.campaigndetail

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import javax.inject.Inject

@HiltViewModel
class CampaignDetailViewModel @Inject constructor(
    private val blazeFeatureUtils: BlazeFeatureUtils,
) : ViewModel() {
    fun start(campaignDetailPageSource: CampaignDetailPageSource) {
        blazeFeatureUtils.trackCampaignDetailsOpened(campaignDetailPageSource)
    }
}

enum class CampaignDetailPageSource(val trackingName: String){
    DASHBOARD_CARD("dashboard_card"),
    CAMPAIGN_LISTING_PAGE("campaign_listing_page"),
    UNKNOWN("unknown")
}

