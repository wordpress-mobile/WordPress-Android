package org.wordpress.android.ui.mysite.cards.blaze

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.mysite.MySiteSource.MySiteRefreshSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.BlazeCardUpdate
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import javax.inject.Inject

class BlazeCardSource @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
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
                        // to do : implement the logic to fetch campaigns
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
