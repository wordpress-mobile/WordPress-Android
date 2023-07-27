package org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail.CampaignDetailPageSource
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.blaze.CampaignStatus
import org.wordpress.android.ui.stats.refresh.utils.ONE_THOUSAND
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

const val CENTS_IN_DOLLARS = 100

@HiltViewModel
class CampaignListingViewModel @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val blazeFeatureUtils: BlazeFeatureUtils,
    private val blazeCampaignsStore: BlazeCampaignsStore,
    private val statsUtils: StatsUtils,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(bgDispatcher) {
    private val _uiState = MutableLiveData<CampaignListingUiState>()
    val uiState: LiveData<CampaignListingUiState> = _uiState

    private val _navigation = MutableLiveData<Event<CampaignListingNavigation>>()
    val navigation = _navigation

    private val _refresh = MutableLiveData<Boolean>()
    val refresh = _refresh

    // todo: Ajesh remove initial message - this is just to show it works
    private val _snackbar = MutableStateFlow<String>("Initial messages")
    val snackBar = _snackbar.asStateFlow()

    private var page = 1

    fun start(campaignListingPageSource: CampaignListingPageSource) {
        blazeFeatureUtils.trackCampaignListingPageShown(campaignListingPageSource)
        _uiState.postValue(CampaignListingUiState.Loading)
        loadCampaigns()
    }

    private fun loadCampaigns() {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            // showNoInternet() error, skipping for now so that loading state can be design reviewed
            return
        }
        launch {
            val blazeCampaignModel = blazeCampaignsStore.getBlazeCampaigns(selectedSiteRepository.getSelectedSite()!!)
            if (blazeCampaignModel.campaigns.isEmpty()) {
                showNoCampaigns()
            } else {
                val campaigns = blazeCampaignModel.campaigns.map {
                    it.mapToCampaignModel()
                }
                showCampaigns(campaigns)
            }
        }
    }

    private fun BlazeCampaignModel.mapToCampaignModel(): CampaignModel {
        return CampaignModel(
            id = this.campaignId.toString(),
            title = UiString.UiStringText(title),
            status = CampaignStatus.fromString(uiStatus),
            featureImageUrl = imageUrl,
            impressions = mapToStatsStringIfNeeded(impressions),
            clicks = mapToStatsStringIfNeeded(clicks),
            budget = convertToDollars(budgetCents)
        )
    }

    private fun mapToStatsStringIfNeeded(value: Long): UiString? {
        return if (value != 0L) {
            val formattedString = statsUtils.toFormattedString(value, ONE_THOUSAND)
            UiString.UiStringText(formattedString)
        } else {
            null
        }
    }

    private fun convertToDollars(budgetCents: Long): UiString {
        return UiString.UiStringText("$" + (budgetCents / CENTS_IN_DOLLARS).toString())
    }

    private fun showCampaigns(campaigns: List<CampaignModel>) {
        _uiState.postValue(
            CampaignListingUiState.Success(
                campaigns,
                this::onCampaignClicked,
                this::createCampaignClick,
                pagingDetails = CampaignListingUiState.Success.PagingDetails(
                    loadMoreFunction = this::loadMoreCampaigns,
                    loadingNext = false
                )
            )
        )
    }

    private fun loadMoreCampaigns() {
        launch {
            if (_uiState.value is CampaignListingUiState.Success &&
                (_uiState.value as CampaignListingUiState.Success).pagingDetails.loadingNext.not()
            ) {
                page++
                showLoadingMore()
                fetchMoreCampaigns()
            }
        }
    }

    private suspend fun fetchMoreCampaigns() {
        val campaignResult = blazeCampaignsStore.fetchBlazeCampaigns(selectedSiteRepository.getSelectedSite()!!, page)
        val currentUiState = _uiState.value as CampaignListingUiState.Success
        val campaigns = campaignResult.model?.campaigns?.map {
            it.mapToCampaignModel()
        }
        if (campaigns.isNullOrEmpty()) {
            disableLoadingMore(currentUiState)
        } else {
            val updatedCampaigns = currentUiState.campaigns + campaigns
            showCampaigns(updatedCampaigns)
        }
    }

    private fun showLoadingMore() {
        val currentUiState = _uiState.value as CampaignListingUiState.Success
        _uiState.postValue(
            currentUiState.copy(
                pagingDetails = CampaignListingUiState.Success.PagingDetails(
                    loadingNext = true
                )
            )
        )
    }

    private fun disableLoadingMore(currentUiState: CampaignListingUiState.Success) {
        _uiState.postValue(
            currentUiState.copy(
                pagingDetails = CampaignListingUiState.Success.PagingDetails(
                    loadingNext = false
                )
            )
        )
    }

    private fun onCampaignClicked(campaignModel: CampaignModel) {
        _navigation.postValue(Event(CampaignListingNavigation.CampaignDetailPage(campaignModel.id.toInt())))
    }

    private fun showNoCampaigns() {
        _uiState.postValue(
            CampaignListingUiState.Error(
                title = UiString.UiStringRes(R.string.campaign_listing_page_no_campaigns_message_title),
                description = UiString.UiStringRes(R.string.campaign_listing_page_no_campaigns_message_description),
                button = CampaignListingUiState.Error.ErrorButton(
                    text = UiString.UiStringRes(R.string.campaign_listing_page_no_campaigns_button_text),
                    click = this::createCampaignClick
                )
            )
        )
    }

    private fun createCampaignClick() {
        _navigation.postValue(Event(CampaignListingNavigation.CampaignCreatePage()))
    }

    fun refreshCampaigns() {
        page = 1
        launch {
            _refresh.postValue(true)
            val blazeCampaignModel =
                blazeCampaignsStore.fetchBlazeCampaigns(selectedSiteRepository.getSelectedSite()!!, page)
            if (blazeCampaignModel.isError) {
                _refresh.postValue(false)
                showSnackBar(R.string.campaign_listing_page_error_could_not_fetch_campaigns)
            } else if (blazeCampaignModel.model?.campaigns.isNullOrEmpty()) {
                _refresh.postValue(false)
            } else if (blazeCampaignModel.model?.campaigns.isNullOrEmpty().not()) {
                _refresh.postValue(false)
                val campaigns = blazeCampaignModel.model?.campaigns?.map {
                    it.mapToCampaignModel()
                }
                showCampaigns(campaigns!!)
            }
        }
    }

    private fun showSnackBar(@StringRes stringRes: Int) {
        launch {
            _snackbar.value = resourceProvider.getString(stringRes).takeIf { it.isNotEmpty() } ?: ""
        }
    }
}

enum class CampaignListingPageSource(val trackingName: String) {
    DASHBOARD_CARD("dashboard_card"),
    MENU_ITEM("menu_item"),
    UNKNOWN("unknown")
}

sealed class CampaignListingNavigation {
    data class CampaignDetailPage(
        val campaignId: Int,
        val campaignDetailPageSource: CampaignDetailPageSource = CampaignDetailPageSource.CAMPAIGN_LISTING_PAGE
    ) : CampaignListingNavigation()

    data class CampaignCreatePage(
        val blazeFlowSource: BlazeFlowSource = BlazeFlowSource.CAMPAIGN_LISTING_PAGE
    ) : CampaignListingNavigation()
}
