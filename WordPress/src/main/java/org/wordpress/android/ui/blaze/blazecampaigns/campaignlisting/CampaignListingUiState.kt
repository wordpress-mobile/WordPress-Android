package org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting

import org.wordpress.android.ui.mysite.cards.blaze.CampaignStatus
import org.wordpress.android.ui.utils.UiString

sealed class CampaignListingUiState {
    data class Loading(
        val image: Int,
        val description: UiString,
    ) : CampaignListingUiState()

    data class Error(
        val title: UiString,
        val description: UiString,
        val button: ErrorButton? = null
    ) : CampaignListingUiState() {
        data class ErrorButton(
            val text: UiString,
            val click: () -> Unit
        )
    }

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


