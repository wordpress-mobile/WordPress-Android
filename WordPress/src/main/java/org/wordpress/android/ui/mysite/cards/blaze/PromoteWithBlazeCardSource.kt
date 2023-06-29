package org.wordpress.android.ui.mysite.cards.blaze

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.mysite.MySiteSource.MySiteRefreshSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.BlazeCardUpdate
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import javax.inject.Inject

class PromoteWithBlazeCardSource @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val blazeFeatureUtils: BlazeFeatureUtils,
) : MySiteRefreshSource<BlazeCardUpdate> {
    override val refresh = MutableLiveData(false)

    override fun build(coroutineScope: CoroutineScope, siteLocalId: Int): LiveData<BlazeCardUpdate> {
        val result = MediatorLiveData<BlazeCardUpdate>()
        result.getData(siteLocalId)
        result.addSource(refresh) { result.refreshData(siteLocalId, refresh.value) }
        refresh()
        return result
    }

    private fun MediatorLiveData<BlazeCardUpdate>.getData(siteLocalId: Int) {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        if (selectedSite != null && selectedSite.id == siteLocalId) {
            if (blazeFeatureUtils.shouldShowBlazeCampaigns(selectedSite)) {
                // to do : implement the logic to fetch campaigns
                postValue(BlazeCardUpdate(true))
            } else if (blazeFeatureUtils.shouldShowBlazeCardEntryPoint(selectedSite)) {
                postValue(BlazeCardUpdate(true))
            } else {
                postValue(BlazeCardUpdate(false))
            }
        } else {
            postErrorState()
        }
    }

    private fun MediatorLiveData<BlazeCardUpdate>.refreshData(
        siteLocalId: Int,
        isRefresh: Boolean? = null
    ) {
        when (isRefresh) {
            null, true -> getData(siteLocalId)
            else -> Unit // Do nothing
        }
    }

    private fun MediatorLiveData<BlazeCardUpdate>.postErrorState() {
        postValue(BlazeCardUpdate(false))
    }
}
