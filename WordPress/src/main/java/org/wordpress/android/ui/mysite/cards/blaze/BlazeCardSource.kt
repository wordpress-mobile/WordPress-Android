package org.wordpress.android.ui.mysite.cards.blaze

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.mysite.MySiteSource.MySiteRefreshSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.BlazeCardUpdate
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import javax.inject.Inject

class BlazeCardSource @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val blazeCampaignsStore: BlazeCampaignsStore,
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
                        val result = blazeCampaignsStore.fetchBlazeCampaigns(selectedSite)
                        // if the request was successful and there are campaigns, show blaze campaigns card
                        if (!result.isError && result.model != null) {
                            val campaign = blazeCampaignsStore.getMostRecentBlazeCampaign(selectedSite)
                            return@launch postState(BlazeCardUpdate(true, campaign = campaign))
                        }
                        // there are no campaigns, show blaze promo card
                        postState(BlazeCardUpdate(true))
                    } else {
                        // show blaze promo card if campaign feature is not available
                        postState(BlazeCardUpdate(true))
                    }
                } else {
                    postState(BlazeCardUpdate(false))
                }
            } else {
                postErrorState()
            }
        }
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
