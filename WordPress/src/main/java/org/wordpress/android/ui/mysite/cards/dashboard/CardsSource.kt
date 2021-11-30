package org.wordpress.android.ui.mysite.cards.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.dashboard.CardsStore
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsResult
import org.wordpress.android.ui.mysite.MySiteSource.MySiteRefreshSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CardsUpdate
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardsSource @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val cardsStore: CardsStore
) : MySiteRefreshSource<CardsUpdate> {
    override val refresh: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    override fun build(coroutineScope: CoroutineScope, siteLocalId: Int): LiveData<CardsUpdate> {
        val result = MediatorLiveData<CardsUpdate>()
        result.refreshData(coroutineScope)
        result.addSource(refresh) { result.refreshData(coroutineScope, refresh.value) }
        return result
    }

    private fun MediatorLiveData<CardsUpdate>.refreshData(
        coroutineScope: CoroutineScope,
        isRefresh: Boolean? = null
    ) {
        when (isRefresh) {
            null, true -> refreshData(coroutineScope)
            else -> Unit // Do nothing
        }
    }

    private fun MediatorLiveData<CardsUpdate>.refreshData(
        coroutineScope: CoroutineScope
    ) {
        coroutineScope.launch {
            postState(CardsUpdate(CardsResult()))
        }
    }
}
