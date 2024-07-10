package org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.wordpress.android.R
import org.wordpress.android.Result
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail.CampaignDetailPageSource
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions")
class CampaignListingViewModel @Inject constructor(
    @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
    private val blazeFeatureUtils: BlazeFeatureUtils,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val resourceProvider: ResourceProvider,
    private val fetchCampaignListUseCase: FetchCampaignListUseCase,
    private val getCampaignListFromDbUseCase: GetCampaignListFromDbUseCase
) : ScopedViewModel(bgDispatcher) {
    private lateinit var site: SiteModel

    private val _uiState = MutableLiveData<CampaignListingUiState>()
    val uiState: LiveData<CampaignListingUiState> = _uiState

    private val _navigation = MutableLiveData<Event<CampaignListingNavigation>>()
    val navigation = _navigation

    private val _refresh = MutableLiveData<Boolean>()
    val refresh = _refresh

    private val _snackbar = MutableSharedFlow<String>()
    val snackBar = _snackbar.asSharedFlow()

    private val _onSelectedSiteMissing = MutableLiveData<Unit>()
    val onSelectedSiteMissing = _onSelectedSiteMissing as LiveData<Unit>

    private var offset = 0
    private var isLastPage: Boolean = false

    fun start(campaignListingPageSource: CampaignListingPageSource) {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            _onSelectedSiteMissing.value = Unit
            return
        }
        this.site = site
        blazeFeatureUtils.trackCampaignListingPageShown(campaignListingPageSource)
        loadCampaigns()
    }

    private fun loadCampaigns() {
        _uiState.postValue(CampaignListingUiState.Loading)
        launch {
            when (val campaigns = getCampaignListFromDbUseCase.execute(site)) {
                is Result.Success -> showCampaigns(campaigns.value)
                is Result.Failure -> fetchCampaigns()
            }
        }
    }

    private suspend fun fetchCampaigns() {
        if (networkUtilsWrapper.isNetworkAvailable().not()) {
            showNoNetworkError()
        } else {
            when (val campaignResult = fetchCampaignListUseCase.execute(site, offset)) {
                is Result.Success -> showCampaigns(campaignResult.value.campaigns)
                is Result.Failure -> {
                    when (campaignResult.value) {
                        is GenericResult -> showGenericError()
                        is NoCampaigns -> showNoCampaigns()
                    }
                }
            }
        }
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

    private fun showGenericError() {
        _uiState.postValue(CampaignListingUiState.Error.GenericError(this@CampaignListingViewModel::loadCampaigns))
    }

    private fun showNoNetworkError() {
        _uiState.postValue(CampaignListingUiState.Error.NoNetworkError(this@CampaignListingViewModel::loadCampaigns))
    }

    private fun loadMoreCampaigns() {
        launch {
            if (_uiState.value is CampaignListingUiState.Success &&
                (_uiState.value as CampaignListingUiState.Success).pagingDetails.loadingNext.not() &&
                isLastPage.not()
            ) {
                showLoadingMore()
                fetchMoreCampaigns()
            }
        }
    }

    private suspend fun fetchMoreCampaigns() {
        if (networkUtilsWrapper.isNetworkAvailable().not()) {
            disableLoadingMore()
            showSnackBar(R.string.campaign_listing_page_error_refresh_no_network_available)
        } else {
            when (val campaignResult = fetchCampaignListUseCase.execute(site, offset)) {
                is Result.Success -> {
                    val currentUiState = _uiState.value as CampaignListingUiState.Success
                    val allCampaigns = currentUiState.campaigns + campaignResult.value.campaigns
                    isLastPage = allCampaigns.size >= campaignResult.value.totalItems
                    offset += allCampaigns.size
                    showCampaigns(allCampaigns)
                }

                is Result.Failure -> {
                    when (campaignResult.value) {
                        is GenericResult -> {
                            disableLoadingMore()
                            showSnackBar(R.string.campaign_listing_page_error_refresh_could_not_fetch_campaigns)
                        }

                        is NoCampaigns -> disableLoadingMore()
                    }
                }
            }
        }
    }

    private fun showLoadingMore() = updateLoadingMoreState(showLoading = true)

    private fun disableLoadingMore() = updateLoadingMoreState(showLoading = false)

    private fun updateLoadingMoreState(showLoading: Boolean = false) {
        val currentUiState = _uiState.value as CampaignListingUiState.Success
        _uiState.postValue(
            currentUiState.copy(
                pagingDetails = CampaignListingUiState.Success.PagingDetails(
                    loadingNext = showLoading
                )
            )
        )
    }

    private fun onCampaignClicked(campaignModel: CampaignModel) {
        _navigation.postValue(Event(CampaignListingNavigation.CampaignDetailPage(campaignModel.id)))
    }

    private fun showNoCampaigns() {
        _uiState.postValue(CampaignListingUiState.Error.NoCampaignsError { createCampaignClick() })
    }

    private fun createCampaignClick() {
        _navigation.postValue(Event(CampaignListingNavigation.CampaignCreatePage()))
    }

    fun refreshCampaigns() {
        launch {
            _refresh.postValue(true)
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                _refresh.postValue(false)
                showSnackBar(R.string.campaign_listing_page_error_refresh_no_network_available)
            } else {
                offset = 0
                when (val campaignResult = fetchCampaignListUseCase.execute(site, offset)) {
                    is Result.Success -> {
                        _refresh.postValue(false)
                        isLastPage = false
                        showCampaigns(campaignResult.value.campaigns)
                    }

                    is Result.Failure -> {
                        when (campaignResult.value) {
                            is GenericResult -> {
                                _refresh.postValue(false)
                                showSnackBar(R.string.campaign_listing_page_error_refresh_could_not_fetch_campaigns)
                            }

                            // on refresh there shouldn't be a case where there are no campaigns
                            is NoCampaigns -> _refresh.postValue(false)
                        }
                    }
                }
            }
        }
    }

    private suspend fun showSnackBar(@StringRes stringRes: Int) {
        _snackbar.emit(resourceProvider.getString(stringRes).takeIf { it.isNotEmpty() } ?: "")
    }
}

enum class CampaignListingPageSource(val trackingName: String) {
    DASHBOARD_CARD("dashboard_card"),
    MENU_ITEM("menu_item"),
    UNKNOWN("unknown")
}

sealed class CampaignListingNavigation {
    data class CampaignDetailPage(
        val campaignId: String,
        val campaignDetailPageSource: CampaignDetailPageSource = CampaignDetailPageSource.CAMPAIGN_LISTING_PAGE
    ) : CampaignListingNavigation()

    data class CampaignCreatePage(
        val blazeFlowSource: BlazeFlowSource = BlazeFlowSource.CAMPAIGN_LISTING_PAGE
    ) : CampaignListingNavigation()
}
