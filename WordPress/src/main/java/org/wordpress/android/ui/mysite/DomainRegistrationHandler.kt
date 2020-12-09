package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore.OnPlansFetched
import org.wordpress.android.ui.plans.isDomainCreditAvailable
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.DOMAIN_REGISTRATION
import org.wordpress.android.util.SiteUtilsWrapper
import javax.inject.Inject

class DomainRegistrationHandler
@Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val siteUtils: SiteUtilsWrapper
) {
    private val _sitePlansFetched = MutableLiveData<OnPlansFetched>()
    val isDomainCreditAvailable: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(selectedSiteRepository.selectedSiteChange) {
            it?.let { site ->
                if (shouldFetchPlans(site)) {
                    fetchPlans(site)
                } else {
                    postValue(false)
                }
            }
        }
        addSource(_sitePlansFetched) { event ->
            if (event.isError) {
                AppLog.e(DOMAIN_REGISTRATION, "An error occurred while fetching plans : " + event.error.message)
            } else if (selectedSiteRepository.getSelectedSite()?.id == event.site.id) {
                postValue(isDomainCreditAvailable(event.plans))
            }
        }
    }

    init {
        dispatcher.register(this)
    }

    fun clear() {
        dispatcher.unregister(this)
    }

    private fun shouldFetchPlans(site: SiteModel) = !siteUtils.onFreePlan(site) && !siteUtils.hasCustomDomain(site)

    private fun fetchPlans(site: SiteModel) = dispatcher.dispatch(SiteActionBuilder.newFetchPlansAction(site))

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlansFetched(event: OnPlansFetched) = _sitePlansFetched.postValue(event)
}
