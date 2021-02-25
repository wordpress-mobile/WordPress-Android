package org.wordpress.android.ui.mysite.dynamiccards

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.fluxc.store.DynamicCardStore
import org.wordpress.android.ui.mysite.MySiteSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.DynamicCardsUpdate
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicCardsSource
@Inject constructor(
    private val dynamicCardStore: DynamicCardStore,
    private val selectedSiteRepository: SelectedSiteRepository
) : MySiteSource<DynamicCardsUpdate> {
    private val refresh = MutableLiveData<Boolean>()

    override fun buildSource(coroutineScope: CoroutineScope, siteId: Int): LiveData<DynamicCardsUpdate> {
        val data = MediatorLiveData<DynamicCardsUpdate>()
        data.refreshData(coroutineScope, siteId)
        data.addSource(refresh) {
            data.refreshData(coroutineScope, siteId)
        }
        return data
    }

    private fun MediatorLiveData<DynamicCardsUpdate>.refreshData(coroutineScope: CoroutineScope, siteId: Int) {
        coroutineScope.launch {
            val cards = dynamicCardStore.getCards(siteId)
            this@refreshData.postValue(
                    DynamicCardsUpdate(
                            pinnedDynamicCard = cards.pinnedItem,
                            cards = cards.dynamicCardTypes
                    )
            )
        }
    }

    suspend fun pinItem(dynamicCardType: DynamicCardType) {
        callWithSite { siteId -> dynamicCardStore.pinCard(siteId, dynamicCardType) }
    }

    suspend fun hideItem(dynamicCardType: DynamicCardType) {
        callWithSite { siteId -> dynamicCardStore.hideCard(siteId, dynamicCardType) }
    }

    suspend fun unpinItem() {
        callWithSite { siteId -> dynamicCardStore.unpinCard(siteId) }
    }

    suspend fun removeItem(dynamicCardType: DynamicCardType) {
        callWithSite { siteId -> dynamicCardStore.removeCard(siteId, dynamicCardType) }
    }

    private suspend fun callWithSite(function: suspend (Int) -> Unit) {
        selectedSiteRepository.getSelectedSite()?.id?.let { siteId ->
            function(siteId)
            refresh.postValue(true)
        }
    }
}
