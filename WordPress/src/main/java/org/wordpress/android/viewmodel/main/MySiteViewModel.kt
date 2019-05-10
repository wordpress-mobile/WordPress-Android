package org.wordpress.android.viewmodel.main

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
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

class MySiteViewModel @Inject constructor(val store: SiteStore, val dispatcher: Dispatcher): ViewModel() {
    private val plans = MutableLiveData<List<PlanModel>>()
    private val site = MutableLiveData<SiteModel>()

    val currentPlan = MediatorLiveData<PlanModel>().apply {
        addSource(plans) { plans ->
            value = plans?.find { it.isCurrentPlan }
        }
    }

    init {
        dispatcher.register(this)
    }

    override fun onCleared() {
        dispatcher.unregister(this)
        super.onCleared()
    }

    fun getSite(): LiveData<SiteModel> {
        return site
    }

    fun setSite(site: SiteModel?) {
        if (site?.id != this.site.value?.id) {
            this.site.value = site
        }
    }

    fun loadPlans(site: SiteModel?) {
        if (site != null) {
            dispatcher.dispatch(SiteActionBuilder.newFetchPlansAction(site))
        }
    }

    fun getPlans(): LiveData<List<PlanModel>> {
        return plans
    }

    fun clearPlans() {
        plans.value = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlansFetched(event: OnPlansFetched) {
        if (!event.isError) {
            plans.value = event.plans
        }
    }
}
