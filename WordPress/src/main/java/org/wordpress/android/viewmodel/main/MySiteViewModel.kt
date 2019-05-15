package org.wordpress.android.viewmodel.main

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.PlanModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnPlansFetched
import javax.inject.Inject

class MySiteViewModel @Inject constructor(val store: SiteStore, val dispatcher: Dispatcher) : ViewModel() {
    init {
        dispatcher.register(this)
    }

    override fun onCleared() {
        dispatcher.unregister(this)
        super.onCleared()
    }

    private val _status = MutableLiveData<NetworkStatus>()
    val status: LiveData<NetworkStatus>
        get() = _status

    private val _site = MutableLiveData<SiteModel>()
    val site: LiveData<SiteModel>
        get() = _site

    private val _plans = MutableLiveData<List<PlanModel>>()
    val plans: LiveData<List<PlanModel>>
        get() = _plans

    val isDomainRegistrationVisible: LiveData<Boolean> = Transformations.map(plans) { plans ->
        plans?.find { it.isCurrentPlan }?.hasDomainCredit ?: false
    }

    fun setSite(site: SiteModel?) {
        if (site?.id != _site.value?.id) {
            _site.value = site
        }
    }

    fun loadPlans(site: SiteModel?) {
        if (site != null) {
            _status.value = NetworkStatus(Status.LOADING)
            dispatcher.dispatch(SiteActionBuilder.newFetchPlansAction(site))
        }
    }

    fun clearPlans() {
        _plans.value = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlansFetched(event: OnPlansFetched) {
        if (!event.isError) {
            _status.value = NetworkStatus(Status.SUCCESS)
            _plans.value = event.plans
        } else {
            _status.value = NetworkStatus(
                    Status.FAILED,
                    "An error occurred while fetching plans : ${event.error.message}"
            )
        }
    }

    data class NetworkStatus(val status: Status, val logMessage: String? = null)

    enum class Status { LOADING, SUCCESS, FAILED }
}
