package org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
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
class CampaignListingViewModel @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val blazeFeatureUtils: BlazeFeatureUtils,
    private val blazeCampaignsStore: BlazeCampaignsStore,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val resourceProvider: ResourceProvider,
    private val mapper: CampaignListingDomainMapper
) : ScopedViewModel(bgDispatcher) {
    lateinit var site: SiteModel

    private val _uiState = MutableLiveData<CampaignListingUiState>()
    val uiState: LiveData<CampaignListingUiState> = _uiState

    private val _navigation = MutableLiveData<Event<CampaignListingNavigation>>()
    val navigation = _navigation

    private val _refresh = MutableLiveData<Boolean>()
    val refresh = _refresh

    private val _snackbar = MutableSharedFlow<String>()
    val snackBar = _snackbar.asSharedFlow()

    private var page = 1

    fun start(campaignListingPageSource: CampaignListingPageSource) {
        this.site = selectedSiteRepository.getSelectedSite()!!
        blazeFeatureUtils.trackCampaignListingPageShown(campaignListingPageSource)
        loadCampaigns()
    }

    private fun loadCampaigns() {
        _uiState.postValue(CampaignListingUiState.Loading)
        launch {
            val blazeCampaignModel = blazeCampaignsStore.getBlazeCampaigns(site)
            if (blazeCampaignModel.campaigns.isEmpty()) {
                if (networkUtilsWrapper.isNetworkAvailable().not()) {
                    _uiState.postValue(mapper.toNoNetworkError(this@CampaignListingViewModel::loadCampaigns))
                } else {
                    val campaignResult = blazeCampaignsStore.fetchBlazeCampaigns(site, page)
                    if (campaignResult.isError) {
                        _uiState.postValue(mapper.toGenericError(this@CampaignListingViewModel::loadCampaigns))
                    } else if (campaignResult.model == null || campaignResult.model?.campaigns.isNullOrEmpty()) {
                        showNoCampaigns()
                    } else {
                        val campaigns = campaignResult.model!!.campaigns.map { mapper.mapToCampaignModel(it) }
                        showCampaigns(campaigns)
                    }
                }
            } else {
                val campaigns = blazeCampaignModel.campaigns.map {
                    mapper.mapToCampaignModel(it)
                }
                showCampaigns(campaigns)
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
        val campaignResult = blazeCampaignsStore.fetchBlazeCampaigns(site, page)
        val currentUiState = _uiState.value as CampaignListingUiState.Success
        val campaigns = campaignResult.model?.campaigns?.map { mapper.mapToCampaignModel(it) }
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            disableLoadingMore()
            showSnackBar(R.string.campaign_listing_page_error_refresh_no_network_available)
        } else if (campaignResult.isError) {
            disableLoadingMore()
            showSnackBar(R.string.campaign_listing_page_error_refresh_could_not_fetch_campaigns)
        } else if (campaigns.isNullOrEmpty()) {
            disableLoadingMore()
        } else {
            val updatedCampaigns = currentUiState.campaigns + campaigns
            showCampaigns(updatedCampaigns)
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
        _navigation.postValue(Event(CampaignListingNavigation.CampaignDetailPage(campaignModel.id.toInt())))
    }

    private fun showNoCampaigns() {
        _uiState.postValue(mapper.toNoCampaignsError { createCampaignClick() })
    }

    private fun createCampaignClick() {
        _navigation.postValue(Event(CampaignListingNavigation.CampaignCreatePage()))
    }

    fun refreshCampaigns() {
        page = 1
        launch {
            _refresh.postValue(true)
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                _refresh.postValue(false)
                showSnackBar(R.string.campaign_listing_page_error_refresh_no_network_available)
            } else {
                val blazeCampaignModel = blazeCampaignsStore.fetchBlazeCampaigns(site, page)
                if (blazeCampaignModel.isError || blazeCampaignModel.model?.campaigns.isNullOrEmpty()) {
                    _refresh.postValue(false)
                    showSnackBar(R.string.campaign_listing_page_error_refresh_could_not_fetch_campaigns)
                } else if (blazeCampaignModel.model?.campaigns.isNullOrEmpty().not()) {
                    _refresh.postValue(false)
                    val campaigns = blazeCampaignModel.model?.campaigns?.map {
                        mapper.mapToCampaignModel(it)
                    }
                    showCampaigns(campaigns!!)
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
        val campaignId: Int,
        val campaignDetailPageSource: CampaignDetailPageSource = CampaignDetailPageSource.CAMPAIGN_LISTING_PAGE
    ) : CampaignListingNavigation()

    data class CampaignCreatePage(
        val blazeFlowSource: BlazeFlowSource = BlazeFlowSource.CAMPAIGN_LISTING_PAGE
    ) : CampaignListingNavigation()
}
