package org.wordpress.android.ui.mysite.cards.blaze

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.blaze.BlazeStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.MySiteSource.MySiteRefreshSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.PromoteWithBlazeUpdate
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.config.BlazeFeatureConfig
import javax.inject.Inject
import javax.inject.Named

const val REFRESH_DELAY = 500L

class PromoteWithBlazeCardSource @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val blazeStore: BlazeStore,
    blazeFeatureConfig: BlazeFeatureConfig,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : MySiteRefreshSource<PromoteWithBlazeUpdate> {
    private val isPromoteWithBlazeEnabled = blazeFeatureConfig.isEnabled()
    override val refresh = MutableLiveData(false)

    override fun build(coroutineScope: CoroutineScope, siteLocalId: Int): LiveData<PromoteWithBlazeUpdate> {
        val result = MediatorLiveData<PromoteWithBlazeUpdate>()
        result.getData(coroutineScope, siteLocalId)
        result.addSource(refresh) { result.refreshData(coroutineScope, siteLocalId, refresh.value) }
        refresh()
        return result
    }

    private fun MediatorLiveData<PromoteWithBlazeUpdate>.getData(
        coroutineScope: CoroutineScope,
        siteLocalId: Int
    ) {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        if (selectedSite != null && selectedSite.id == siteLocalId) {
            coroutineScope.launch(bgDispatcher) {
                if (isPromoteWithBlazeEnabled) {
                    blazeStore.getBlazeStatus(selectedSite.siteId)
                        .map { it.model?.firstOrNull() }
                        .collect { result ->
                            postValue(PromoteWithBlazeUpdate(result))
                        }
                } else {
                    postEmptyState()
                }
            }
        } else {
            postErrorState()
        }
    }

    private fun MediatorLiveData<PromoteWithBlazeUpdate>.refreshData(
        coroutineScope: CoroutineScope,
        siteLocalId: Int,
        isRefresh: Boolean? = null
    ) {
        when (isRefresh) {
            null, true -> refreshData(coroutineScope, siteLocalId)
            else -> Unit // Do nothing
        }
    }

    private fun MediatorLiveData<PromoteWithBlazeUpdate>.refreshData(
        coroutineScope: CoroutineScope,
        siteLocalId: Int
    ) {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        if (selectedSite != null && selectedSite.id == siteLocalId) {
            fetchBlazeStatusAndPostErrorIfAvailable(coroutineScope, selectedSite)
        } else {
            postErrorState()
        }
    }

    private fun MediatorLiveData<PromoteWithBlazeUpdate>.fetchBlazeStatusAndPostErrorIfAvailable(
        coroutineScope: CoroutineScope,
        selectedSite: SiteModel
    ) {
        coroutineScope.launch(bgDispatcher) {
            delay(REFRESH_DELAY)
            val result = blazeStore.fetchBlazeStatus(selectedSite)
            val model = result.model
            val error = result.error
            when {
                error != null -> postErrorState()
                model != null -> postLastState()
                else -> postLastState()
            }
        }
    }

    /**
     * This function is used to make sure the [refresh] information is propagated and processed correctly even though
     * the previous status is still the current one. This avoids issues like the loading progress indicator being shown
     * indefinitely.
     */
    private fun MediatorLiveData<PromoteWithBlazeUpdate>.postLastState() {
        val lastStatus = value?.blazeStatusModel
        postState(PromoteWithBlazeUpdate(lastStatus))
    }

    private fun MediatorLiveData<PromoteWithBlazeUpdate>.postErrorState() {
        postState(PromoteWithBlazeUpdate(null))
    }

    private fun MediatorLiveData<PromoteWithBlazeUpdate>.postEmptyState() {
        postState(PromoteWithBlazeUpdate(null))
    }
}
