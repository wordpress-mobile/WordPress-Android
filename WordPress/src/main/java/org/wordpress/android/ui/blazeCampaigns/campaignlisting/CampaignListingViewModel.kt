package org.wordpress.android.ui.blazeCampaigns.campaignlisting

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.mysite.cards.blaze.CampaignStatus
import org.wordpress.android.ui.utils.UiString
import javax.inject.Inject

@HiltViewModel
class CampaignListingViewModel @Inject constructor(
    private val blazeFeatureUtils: BlazeFeatureUtils,
) : ViewModel() {

    private val _uiState = MutableLiveData<CampaignListingUiState>()
    val uiState: MutableLiveData<CampaignListingUiState> = _uiState

    fun start(campaignListingPageSource: CampaignListingPageSource) {
        _uiState.value = CampaignListingUiState.Loading
    }

    fun getCampaigns() {
        // todo: get campaigns from store and update uiState
    }
}

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

enum class CampaignListingPageSource(val trackingName: String){
    DASHBOARD_CARD("dashboard_card"),
    MENU_ITEM("menu_item")
}

