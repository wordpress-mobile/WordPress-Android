package org.wordpress.android.ui.plans

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PlanOffersAction.FETCH_PLAN_OFFERS
import org.wordpress.android.fluxc.generated.PlanOffersActionBuilder
import org.wordpress.android.fluxc.model.plans.PlanOffersModel
import org.wordpress.android.fluxc.store.PlanOffersStore
import org.wordpress.android.fluxc.store.PlanOffersStore.OnPlanOffersFetched
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.plans.PlansViewModel.PlansListStatus.DONE
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

class PlansViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    @Suppress("unused")
    private var plansStore: PlanOffersStore,
    @param:Named(UI_THREAD) private val uiDispatcher: CoroutineDispatcher,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(uiDispatcher) {
    enum class PlansListStatus {
        DONE,
        ERROR,
        ERROR_WITH_CACHE,
        FETCHING
    }

    private val _listStatus = MutableLiveData<PlansListStatus>()
    val listStatus: LiveData<PlansListStatus>
        get() = _listStatus

    private val _plans = MutableLiveData<List<PlanOffersModel>?>()
    private val _cachedPlans = MutableLiveData<List<PlanOffersModel>?>()
    val plans: LiveData<List<PlanOffersModel>?>
        get() = _plans

    private val _showDialog = SingleLiveEvent<PlanOffersModel>()
    val showDialog: LiveData<PlanOffersModel>
        get() = _showDialog

    private var isStarted = false

    init {
        dispatcher.register(this)
    }

    fun create() {
        if (isStarted) {
            return
        }
        fetchPlans()
        isStarted = true
    }

    private fun fetchPlans() {
        _listStatus.value = PlansListStatus.FETCHING
        launch {
            dispatcher.dispatch(PlanOffersActionBuilder.generateNoPayloadAction(FETCH_PLAN_OFFERS))
        }
    }

    override fun onCleared() {
        dispatcher.unregister(this)
        super.onCleared()
    }

    fun onItemClicked(item: PlanOffersModel) {
        analyticsTrackerWrapper.track(Stat.OPENED_PLANS_COMPARISON)
        _showDialog.value = item
    }

    fun onPullToRefresh() {
        fetchPlans()
    }

    fun onShowCachedPlansButtonClicked() {
        _listStatus.value = DONE
        _plans.value = _cachedPlans.value
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlanOffersFetched(event: OnPlanOffersFetched) {
        if (event.isError && event.planOffers?.isEmpty() == false) {
            _listStatus.value = PlansListStatus.ERROR_WITH_CACHE
            _cachedPlans.value = event.planOffers
            _plans.value = emptyList()
            AppLog.e(T.API, "An error occurred while fetching plans. Cache is available.")
        } else if (event.isError) {
            _listStatus.value = PlansListStatus.ERROR
            _plans.value = emptyList()
            AppLog.e(T.API, "An error occurred while fetching plans. Cache is not available.")
        } else {
            _listStatus.value = DONE
            _plans.value = event.planOffers
        }
    }
}
