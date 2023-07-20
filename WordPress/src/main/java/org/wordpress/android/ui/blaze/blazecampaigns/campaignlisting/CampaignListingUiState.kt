package org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting

import org.wordpress.android.ui.mysite.cards.blaze.CampaignStatus
import org.wordpress.android.ui.utils.UiString

sealed class CampaignListingUiState {
    object Loading : CampaignListingUiState()
    data class Error(val error: String) : CampaignListingUiState()
    data class Success(val campaigns: List<CampaignModel>) : CampaignListingUiState()
}

data class CampaignModel(
    val id: String,
    val title: String,
    val status: CampaignStatus,
    val featureImageUrl: String,
    val impressions: UiString?,
    val clicks: UiString?,
    val budget: UiString,
)

