package org.wordpress.android.ui.mysite.cards.blaze

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.Result
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.FetchCampaignListUseCase
import org.wordpress.android.ui.mysite.MySiteSource.MySiteRefreshSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.BlazeCardUpdate
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

class BlazeCardSource @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val fetchCampaignListUseCase: FetchCampaignListUseCase,
    private val mostRecentCampaignUseCase: MostRecentCampaignUseCase,
    private val blazeFeatureUtils: BlazeFeatureUtils,
) : MySiteRefreshSource<BlazeCardUpdate> {
    override val refresh = MutableLiveData(false)

    override fun build(coroutineScope: CoroutineScope, siteLocalId: Int): LiveData<BlazeCardUpdate> {
        val result = MediatorLiveData<BlazeCardUpdate>()
        refresh()
        result.getData(coroutineScope, siteLocalId)
        result.addSource(refresh) { result.refreshData(coroutineScope, siteLocalId, refresh.value) }
        return result
    }

    private fun MediatorLiveData<BlazeCardUpdate>.getData(coroutineScope: CoroutineScope, siteLocalId: Int) {
        coroutineScope.launch {
            val selectedSite = selectedSiteRepository.getSelectedSite()
            if (selectedSite != null && selectedSite.id == siteLocalId) {
                if (blazeFeatureUtils.shouldShowBlazeCardEntryPoint(selectedSite)) {
                    if (blazeFeatureUtils.shouldShowBlazeCampaigns()) {
                        fetchCampaigns(selectedSite)
                    } else {
                        // show blaze promo card if campaign feature is not available
                       showPromoteWithBlazeCard()
                    }
                } else {
                    postState(BlazeCardUpdate(false))
                }
            } else {
                postErrorState()
            }
        }
    }

    private suspend fun MediatorLiveData<BlazeCardUpdate>.fetchCampaigns(site: SiteModel) {
        if (networkUtilsWrapper.isNetworkAvailable().not()) {
            getMostRecentCampaignFromDb(site)
        } else {
            when (fetchCampaignListUseCase.execute(site = site, page = 1)) {
                is Result.Success -> getMostRecentCampaignFromDb(site)
                // there are no campaigns or if there is an error , show blaze promo card
                is Result.Failure -> showPromoteWithBlazeCard()
            }
        }
    }

    private suspend fun MediatorLiveData<BlazeCardUpdate>.getMostRecentCampaignFromDb(site: SiteModel) {
        when(val result = mostRecentCampaignUseCase.execute(site)) {
            is Result.Success -> postState(BlazeCardUpdate(true, campaign = result.value))
            is Result.Failure -> showPromoteWithBlazeCard()
        }
    }

    private fun MediatorLiveData<BlazeCardUpdate>.showPromoteWithBlazeCard() {
        postState(BlazeCardUpdate(true))
    }

    private fun MediatorLiveData<BlazeCardUpdate>.refreshData(
        coroutineScope: CoroutineScope,
        siteLocalId: Int,
        isRefresh: Boolean? = null
    ) {
        when (isRefresh) {
            null, true -> getData(coroutineScope, siteLocalId)
            else -> Unit // Do nothing
        }
    }

    private fun MediatorLiveData<BlazeCardUpdate>.postErrorState() {
        postState(BlazeCardUpdate(false))
    }
}
