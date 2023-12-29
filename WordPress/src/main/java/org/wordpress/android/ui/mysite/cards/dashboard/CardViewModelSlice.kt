package org.wordpress.android.ui.mysite.cards.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.Type
import org.wordpress.android.fluxc.store.dashboard.CardsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.dashboard.activity.ActivityLogCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.dashboard.activity.DashboardActivityLogCardFeatureUtils
import org.wordpress.android.ui.mysite.cards.dashboard.pages.PagesCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostsCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.dashboard.todaysstats.TodaysStatsViewModelSlice
import org.wordpress.android.ui.mysite.cards.dynamiccard.DynamicCardsViewModelSlice
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.config.DynamicDashboardCardsFeatureConfig
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Named

const val REFRESH_DELAY = 500L

class CardViewModelSlice @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val cardsStore: CardsStore,
    private val dashboardActivityLogCardFeatureUtils: DashboardActivityLogCardFeatureUtils,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val dynamicDashboardCardsFeatureConfig: DynamicDashboardCardsFeatureConfig,
    private val pagesCardViewModelSlice: PagesCardViewModelSlice,
    private val dynamicCardsViewModelSlice: DynamicCardsViewModelSlice,
    private val todaysStatsViewModelSlice: TodaysStatsViewModelSlice,
    private val postsCardViewModelSlice: PostsCardViewModelSlice,
    private val activityLogCardViewModelSlice: ActivityLogCardViewModelSlice,
) {
    private lateinit var scope: CoroutineScope

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _uiModel = MutableLiveData<CardsState>()
    val uiModel: LiveData<CardsState> = _uiModel

    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = merge(
        _onNavigation,
        pagesCardViewModelSlice.onNavigation,
        todaysStatsViewModelSlice.onNavigation,
        postsCardViewModelSlice.onNavigation,
        activityLogCardViewModelSlice.onNavigation,
        dynamicCardsViewModelSlice.onNavigation,
    )

    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    val onSnackbarMessage = _onSnackbarMessage

    private val _refresh = MutableLiveData<Event<Boolean>>()
    val refresh =
        merge(
            _refresh,
            pagesCardViewModelSlice.refresh,
            todaysStatsViewModelSlice.refresh,
            postsCardViewModelSlice.refresh,
            activityLogCardViewModelSlice.refresh,
            dynamicCardsViewModelSlice.refresh,
        )


    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    private fun getData(
        siteLocalId: Int
    ) {
        _isRefreshing.postValue(true)
        val selectedSite = selectedSiteRepository.getSelectedSite()
        if (selectedSite != null && selectedSite.id == siteLocalId) {
            scope.launch(bgDispatcher) {
                cardsStore.getCards(selectedSite)
                    .map { it.model }
                    .map { cards -> cards?.filter { getCardTypes(selectedSite).contains(it.type) } }
                    .collect { result ->
                        postState(result)
                    }
            }
        } else {
            postErrorState()
        }
    }

    private fun refreshData(
        siteLocalId: Int
    ) {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        if (selectedSite != null && selectedSite.id == siteLocalId) {
            fetchCardsAndPostErrorIfAvailable(selectedSite)
        } else {
            postErrorState()
        }
    }

    private fun fetchCardsAndPostErrorIfAvailable(
        selectedSite: SiteModel
    ) {
        scope.launch(bgDispatcher) {
            delay(REFRESH_DELAY)
            val result = cardsStore.fetchCards(selectedSite, getCardTypes(selectedSite))
            val model = result.model
            val error = result.error
            when {
                error != null -> postErrorState()
                else -> _refresh.postValue(Event(false))
            }
        }
    }

    private fun getCardTypes(selectedSite: SiteModel) = mutableListOf<Type>().apply {
        if (shouldRequestStatsCard(selectedSite)) add(Type.TODAYS_STATS)
        if (shouldRequestPagesCard(selectedSite)) add(Type.PAGES)
        if (dashboardActivityLogCardFeatureUtils.shouldRequestActivityCard(selectedSite)) add(Type.ACTIVITY)
        add(Type.POSTS)
        if (shouldRequestDynamicCards()) add(Type.DYNAMIC)
    }.toList()

    private fun shouldRequestPagesCard(selectedSite: SiteModel): Boolean {
        return (selectedSite.hasCapabilityEditPages || selectedSite.isSelfHostedAdmin) &&
                !appPrefsWrapper.getShouldHidePagesDashboardCard(selectedSite.siteId)
    }

    private fun shouldRequestStatsCard(selectedSite: SiteModel): Boolean {
        return !appPrefsWrapper.getShouldHideTodaysStatsDashboardCard(selectedSite.siteId)
    }

    private fun shouldRequestDynamicCards(): Boolean {
        return dynamicDashboardCardsFeatureConfig.isEnabled()
    }

    private fun postErrorState() {
        if (_uiModel.value != null && _uiModel.value is CardsState.ErrorState) {
            _onSnackbarMessage.postValue(
                Event(
                    SnackbarMessageHolder(UiString.UiStringRes(R.string.my_site_dashboard_update_error))
                )
            )
            _uiModel.postValue(
                CardsState.ErrorState(
                    MySiteCardAndItem.Item.InfoItem(
                        title = UiString.UiStringRes(R.string.my_site_dashboard_stale_message)
                    )
                )
            )
        }

        if (_uiModel.value == null) {
            _uiModel.postValue(
                CardsState.ErrorState(
                    MySiteCardAndItem.Card.ErrorCard(
                        onRetryClick = ListItemInteraction.create(this::onDashboardErrorRetry)
                    )
                )
            )
        }
        _isRefreshing.postValue(false)
    }

    private fun onDashboardErrorRetry() {
        _refresh.postValue(Event(true))
    }

    fun postState(cards: List<CardModel>?) {
        _isRefreshing.postValue(false)
        if (cards.isNullOrEmpty()) return
        scope.launch {
            val result = mutableListOf<MySiteCardAndItem.Card>()

            val topDynamicCards = async {
                dynamicCardsViewModelSlice.buildTopDynamicCards(
                    cards.firstOrNull { it is CardModel.DynamicCardsModel } as? CardModel.DynamicCardsModel
                )
            }

            val todayStatsCard = async {
                todaysStatsViewModelSlice.buildTodaysStatsCard(
                    cards.firstOrNull { it is CardModel.TodaysStatsCardModel } as? CardModel.TodaysStatsCardModel
                )
            }

            val postCard = async {
                postsCardViewModelSlice.buildPostCard(
                    cards.firstOrNull { it is CardModel.PostsCardModel } as? CardModel.PostsCardModel
                )
            }

            val pagesCard = async {
                pagesCardViewModelSlice.buildCard(
                    cards.firstOrNull { it is CardModel.PagesCardModel } as? CardModel.PagesCardModel
                )
            }

            val activityCard = async {
                activityLogCardViewModelSlice.buildCard(
                    cards.firstOrNull { it is CardModel.ActivityCardModel } as? CardModel.ActivityCardModel
                )
            }

            val bottomDynamicCards = async {
                dynamicCardsViewModelSlice.buildBottomDynamicCards(
                    cards.firstOrNull { it is CardModel.DynamicCardsModel } as? CardModel.DynamicCardsModel
                )
            }

            result.apply {
                topDynamicCards.await()?.let { addAll(it) }
                todayStatsCard.await()?.let { add(it) }
                postCard.await().let { addAll(it) }
                pagesCard.await()?.let { add(it) }
                activityCard.await()?.let { add(it) }
                bottomDynamicCards.await()?.let { addAll(it) }
            }.toList()

            _uiModel.postValue(CardsState.Success(result))
        }
    }
}

sealed class CardsState {
    data class Success(val cards: List<MySiteCardAndItem.Card>) : CardsState()
    data class ErrorState(val error: MySiteCardAndItem) : CardsState()
}
