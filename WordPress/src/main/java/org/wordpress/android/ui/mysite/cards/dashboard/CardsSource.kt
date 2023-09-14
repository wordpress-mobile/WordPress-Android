package org.wordpress.android.ui.mysite.cards.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.Type
import org.wordpress.android.fluxc.store.dashboard.CardsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.MySiteSource.MySiteRefreshSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CardsUpdate
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.dashboard.activity.DashboardActivityLogCardFeatureUtils
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import javax.inject.Inject
import javax.inject.Named

const val REFRESH_DELAY = 500L

class CardsSource @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val cardsStore: CardsStore,
    private val dashboardActivityLogCardFeatureUtils: DashboardActivityLogCardFeatureUtils,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val appPrefsWrapper: AppPrefsWrapper
) : MySiteRefreshSource<CardsUpdate> {
    override val refresh = MutableLiveData(false)

    override fun build(coroutineScope: CoroutineScope, siteLocalId: Int): LiveData<CardsUpdate> {
        val result = MediatorLiveData<CardsUpdate>()
        result.getData(coroutineScope, siteLocalId)
        result.addSource(refresh) { result.refreshData(coroutineScope, siteLocalId, refresh.value) }
        refresh()
        return result
    }

    private fun MediatorLiveData<CardsUpdate>.getData(
        coroutineScope: CoroutineScope,
        siteLocalId: Int
    ) {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        if (selectedSite != null && selectedSite.id == siteLocalId) {
            coroutineScope.launch(bgDispatcher) {
                cardsStore.getCards(selectedSite)
                    .map { it.model }
                    .map { cards -> cards?.filter { getCardTypes(selectedSite).contains(it.type) } }
                    .collect { result ->
                        postValue(CardsUpdate(result))
                    }
            }
        } else {
            postErrorState()
        }
    }

    private fun MediatorLiveData<CardsUpdate>.refreshData(
        coroutineScope: CoroutineScope,
        siteLocalId: Int,
        isRefresh: Boolean? = null
    ) {
        when (isRefresh) {
            null, true -> refreshData(coroutineScope, siteLocalId)
            else -> Unit // Do nothing
        }
    }

    private fun MediatorLiveData<CardsUpdate>.refreshData(
        coroutineScope: CoroutineScope,
        siteLocalId: Int
    ) {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        if (selectedSite != null && selectedSite.id == siteLocalId) {
            fetchCardsAndPostErrorIfAvailable(coroutineScope, selectedSite)
        } else {
            postErrorState()
        }
    }

    private fun MediatorLiveData<CardsUpdate>.fetchCardsAndPostErrorIfAvailable(
        coroutineScope: CoroutineScope,
        selectedSite: SiteModel
    ) {
        coroutineScope.launch(bgDispatcher) {
            delay(REFRESH_DELAY)
            val result = cardsStore.fetchCards(selectedSite, getCardTypes(selectedSite))
            val model = result.model
            val error = result.error
            when {
                error != null -> postErrorState()
                model != null -> onRefreshedBackgroundThread()
                else -> onRefreshedBackgroundThread()
            }
        }
    }

    private fun getCardTypes(selectedSite: SiteModel) = mutableListOf<Type>().apply {
        if (shouldRequestStatsCard(selectedSite)) add(Type.TODAYS_STATS)
        if (shouldRequestPagesCard(selectedSite)) add(Type.PAGES)
        if (dashboardActivityLogCardFeatureUtils.shouldRequestActivityCard(selectedSite)) add(Type.ACTIVITY)
        add(Type.POSTS)
    }.toList()

    private fun shouldRequestPagesCard(selectedSite: SiteModel): Boolean {
        return (selectedSite.hasCapabilityEditPages || selectedSite.isSelfHostedAdmin) &&
                !appPrefsWrapper.getShouldHidePagesDashboardCard(selectedSite.siteId)
    }

    private fun shouldRequestStatsCard(selectedSite: SiteModel): Boolean {
        return !appPrefsWrapper.getShouldHideTodaysStatsDashboardCard(selectedSite.siteId)
    }

    private fun MediatorLiveData<CardsUpdate>.postErrorState() {
        val lastStateCards = this.value?.cards
        val showErrorCard = lastStateCards.isNullOrEmpty()
        val showError = lastStateCards?.isNotEmpty() == true
        postState(
            CardsUpdate(
                cards = lastStateCards,
                showErrorCard = showErrorCard,
                showSnackbarError = showError,
                showStaleMessage = showError
            )
        )
    }
}
