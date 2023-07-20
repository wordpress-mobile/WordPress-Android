package org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting

import org.wordpress.android.ui.mysite.cards.blaze.CampaignStatus
import org.wordpress.android.ui.utils.UiString

sealed class CampaignListingUiState {
    data class Loading(
        val loadingImage: Int,
        val description: UiString,
    ) : CampaignListingUiState()

    data class Error(
        val errorTitle: UiString,
        val errorDescription: UiString,
        val errorButton: ErrorButton
    ) : CampaignListingUiState() {
        data class ErrorButton(
            val buttonTitle: UiString,
            val buttonAction: () -> Unit
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


