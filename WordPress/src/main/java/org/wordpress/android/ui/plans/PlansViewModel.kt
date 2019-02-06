package org.wordpress.android.ui.plans

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PlanOffersAction.FETCH_PLAN_OFFERS
import org.wordpress.android.fluxc.generated.PlanOffersActionBuilder
import org.wordpress.android.fluxc.model.plans.PlanOffersModel
import org.wordpress.android.fluxc.store.PlanOffersStore
import org.wordpress.android.fluxc.store.PlanOffersStore.OnPlanOffersFetched
import org.wordpress.android.modules.UI_SCOPE
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

class PlansViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    @Suppress("unused")
    private var planOffersStore: PlanOffersStore,
    @param:Named(UI_SCOPE) private val uiScope: CoroutineScope
) : ViewModel() {
    enum class PlanOffersListStatus {
        DONE,
        ERROR,
        FETCHING
    }

    private val _listStatus = MutableLiveData<PlanOffersListStatus>()
    val listStatus: LiveData<PlanOffersListStatus>
        get() = _listStatus

    private val _planOffers = MutableLiveData<List<PlanOffersModel>>()
    val planOffers: LiveData<List<PlanOffersModel>>
        get() = _planOffers

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

        fetchPlanOffers()

        isStarted = true
    }

    private fun fetchPlanOffers() {
        _listStatus.value = PlanOffersListStatus.FETCHING
        uiScope.launch {
            dispatcher.dispatch(PlanOffersActionBuilder.generateNoPayloadAction(FETCH_PLAN_OFFERS))
        }
    }

    override fun onCleared() {
        dispatcher.unregister(this)
        super.onCleared()
    }

    fun onItemClicked(item: PlanOffersModel) {
        _showDialog.value = item
    }

    fun onPullToRefresh() {
        fetchPlanOffers()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onPlanOffersFetched(event: OnPlanOffersFetched) {
        if (event.isError) {
            _listStatus.value = PlanOffersListStatus.ERROR
            AppLog.e(T.API, "An error occurred while fetching plans")
        } else {
            _listStatus.value = PlanOffersListStatus.DONE
        }
        _planOffers.value = event.planOffers
    }
}
