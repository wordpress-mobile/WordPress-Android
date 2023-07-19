package org.wordpress.android.ui.blazeCampaigns.campaignlisting

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import javax.inject.Inject

@HiltViewModel
class CampaignListingViewModel @Inject constructor(
    private val blazeFeatureUtils: BlazeFeatureUtils,
) : ViewModel() {
    fun start(campaignListingPageSource: CampaignListingPageSource) {
        blazeFeatureUtils.trackCampaignListingPageShown(campaignListingPageSource)
    }
}

enum class CampaignListingPageSource(val trackingName: String){
    DASHBOARD_CARD("dashboard_card"),
    MENU_ITEM("menu_item"),
    UNKNOWN("unknown")
}

