package org.wordpress.android.ui.mysite.cards.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.Type
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.dashboard.CardsStore
import org.wordpress.android.fluxc.utils.PreferenceUtils
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.MySiteCardAndItem
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
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.DynamicDashboardCardsFeatureConfig
import org.wordpress.android.util.config.FEATURE_FLAG_PLATFORM_PARAMETER
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

@Suppress("LongParameterList")
class CardViewModelSlice @Inject constructor(
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
    private val preferences: PreferenceUtils.PreferenceUtilsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val cardsTracker: CardsTracker
) {
    private lateinit var scope: CoroutineScope

    private var collectJob: Job? = null
    private var fetchJob: Job? = null

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    val uiModel: MutableLiveData<CardsState> = merge(
        dynamicCardsViewModelSlice.topDynamicCards,
        todaysStatsViewModelSlice.uiModel,
        pagesCardViewModelSlice.uiModel,
        postsCardViewModelSlice.uiModel,
        activityLogCardViewModelSlice.uiModel,
        dynamicCardsViewModelSlice.bottomDynamicCards
    ) { topDynamicCards, todaysStatsCard, pagesCard, postsCard, activityCard, bottomDynamicCards ->
        val state = mergeUiModels(
            topDynamicCards,
            todaysStatsCard,
            pagesCard,
            postsCard,
            activityCard,
            bottomDynamicCards
        )
        state
    } as MutableLiveData<CardsState>

    private fun mergeUiModels(
        topDynamicCards: List<MySiteCardAndItem.Card>?,
        todaysStatsCard: MySiteCardAndItem.Card.TodaysStatsCard?,
        pagesCard: MySiteCardAndItem.Card.PagesCard?,
        postsCard: List<MySiteCardAndItem.Card>?,
        activityCard: MySiteCardAndItem.Card.ActivityCard?,
        bottomDynamicCards: List<MySiteCardAndItem.Card>?
    ): CardsState {
        val cards = mutableListOf<MySiteCardAndItem.Card>()
        todaysStatsCard?.let { cards.add(todaysStatsCard) }
        postsCard?.let { cards.addAll(postsCard) }
        pagesCard?.let { cards.add(pagesCard) }
        activityCard?.let { cards.add(activityCard) }
        return CardsState.Success(topDynamicCards ?: emptyList(), cards, bottomDynamicCards ?: emptyList())
    }

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
            dynamicCardsViewModelSlice.refresh,
        )


    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    fun buildCard(
        siteModel: SiteModel
    ) {
        _isRefreshing.postValue(true)
        // fetch data from store and then refresh the data from the server
        collectJob?.cancel()
        collectJob = scope.launch(bgDispatcher) {
            cardsStore.getCards(siteModel)
                .map { it.model }
                .map { cards -> cards?.filter { getCardTypes(siteModel).contains(it.type) } }
                .collect { result ->
                    postState(result)
                }
        }
        fetchCardsAndPostErrorIfAvailable(siteModel)
    }

    private fun fetchCardsAndPostErrorIfAvailable(
        selectedSite: SiteModel
    ) {
        _isRefreshing.postValue(true)
        fetchJob?.cancel()
        fetchJob = scope.launch(bgDispatcher) {
            val payload = CardsRestClient.FetchCardsPayload(
                selectedSite,
                getCardTypes(selectedSite),
                buildNumber = buildConfigWrapper.getAppVersionCode().toString(),
                deviceId = preferences.getFluxCPreferences().getString(NotificationStore.WPCOM_PUSH_DEVICE_UUID, null)
                    ?: generateAndStoreUUID(),
                identifier = buildConfigWrapper.getApplicationId(),
                marketingVersion = buildConfigWrapper.getAppVersionName(),
                platform = FEATURE_FLAG_PLATFORM_PARAMETER,
                osVersion = buildConfigWrapper.androidVersion
            )
            val result = cardsStore.fetchCards(payload)
            val error = result.error
            when {
                error != null -> postErrorState()
                else -> _isRefreshing.postValue(false)
            }
        }
    }

    private fun generateAndStoreUUID(): String {
        return UUID.randomUUID().toString().also {
            preferences.getFluxCPreferences().edit().putString(NotificationStore.WPCOM_PUSH_DEVICE_UUID, it).apply()
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
        if ((uiModel.value == null) || isUiModelEmpty()) {
            // if the
            uiModel.postValue(
                CardsState.ErrorState(
                    MySiteCardAndItem.Card.ErrorCard(
                        onRetryClick = ListItemInteraction.create(this::onDashboardErrorRetry)
                    )
                )
            )
        } else if (uiModel.value is CardsState.ErrorState) {
            // if the error state is already posted, then post the snackbar message
            _onSnackbarMessage.postValue(
                Event(
                    SnackbarMessageHolder(UiString.UiStringRes(R.string.my_site_dashboard_update_error))
                )
            )
        }
        _isRefreshing.postValue(false)
    }

    private fun isUiModelEmpty(): Boolean {
        return (uiModel.value is CardsState.Success)
                && (uiModel.value as CardsState.Success).topCards.isEmpty()
                && (uiModel.value as CardsState.Success).cards.isEmpty()
                && (uiModel.value as CardsState.Success).bottomCards.isEmpty()
    }

    private fun onDashboardErrorRetry() {
        _refresh.postValue(Event(true))
    }

    fun postState(cards: List<CardModel>?) {
        _isRefreshing.postValue(false)
        if (cards.isNullOrEmpty()) {
            uiModel.postValue(CardsState.Success(emptyList(), emptyList(), emptyList()))
            return
        }
        scope.launch(bgDispatcher) {
            dynamicCardsViewModelSlice.buildTopDynamicCards(
                cards.firstOrNull { it is CardModel.DynamicCardsModel } as? CardModel.DynamicCardsModel
            )


            todaysStatsViewModelSlice.buildTodaysStatsCard(
                cards.firstOrNull { it is CardModel.TodaysStatsCardModel } as? CardModel.TodaysStatsCardModel
            )

            postsCardViewModelSlice.buildPostCard(
                cards.firstOrNull { it is CardModel.PostsCardModel } as? CardModel.PostsCardModel
            )

            pagesCardViewModelSlice.buildCard(
                cards.firstOrNull { it is CardModel.PagesCardModel } as? CardModel.PagesCardModel
            )

            activityLogCardViewModelSlice.buildCard(
                cards.firstOrNull { it is CardModel.ActivityCardModel } as? CardModel.ActivityCardModel
            )

            dynamicCardsViewModelSlice.buildBottomDynamicCards(
                cards.firstOrNull { it is CardModel.DynamicCardsModel } as? CardModel.DynamicCardsModel
            )
        }
    }

    fun clearValue() {
        uiModel.postValue(CardsState.Success(emptyList(), emptyList(), emptyList()))
        collectJob?.cancel()
        fetchJob?.cancel()
        dynamicCardsViewModelSlice.clearValue()
        todaysStatsViewModelSlice.clearValue()
        pagesCardViewModelSlice.clearValue()
        postsCardViewModelSlice.clearValue()
        activityLogCardViewModelSlice.clearValue()
    }

    fun trackCardShown(cards: List<MySiteCardAndItem.Card>) {
        cards.filterIsInstance<MySiteCardAndItem.Card.Dynamic>().forEach {
            dynamicCardsViewModelSlice.trackShown(it.id)
        }
        cardsTracker.trackShown(cards)
    }

    fun resetShownTracker() {
        dynamicCardsViewModelSlice.resetShown()
        cardsTracker.resetShown()
    }
}

sealed class CardsState {
    data class Success(
        val topCards: List<MySiteCardAndItem.Card>,
        val cards: List<MySiteCardAndItem.Card>,
        val bottomCards: List<MySiteCardAndItem.Card>
    ) : CardsState()
    data class ErrorState(val error: MySiteCardAndItem) : CardsState()
}
