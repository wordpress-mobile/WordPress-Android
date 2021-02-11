package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore.OnPlansFetched
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.DomainCreditAvailable
import org.wordpress.android.ui.plans.isDomainCreditAvailable
import org.wordpress.android.util.AppLog.T.DOMAIN_REGISTRATION
import org.wordpress.android.util.SiteUtilsWrapper
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.resume

class DomainRegistrationHandler
@Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val dispatcher: Dispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val appLogWrapper: AppLogWrapper,
    private val siteUtils: SiteUtilsWrapper
) : MySiteSource<DomainCreditAvailable> {
    private var continuation: CancellableContinuation<OnPlansFetched>? = null

    override fun buildSource(coroutineScope: CoroutineScope, siteId: Int): LiveData<DomainCreditAvailable> {
        continuation?.cancel()
        continuation = null
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null || site.id != siteId) {
            return MutableLiveData()
        }
        if (shouldFetchPlans(site)) {
            val result = MutableLiveData<DomainCreditAvailable>()
            coroutineScope.launch(bgDispatcher) {
                try {
                    val event = suspendCancellableCoroutine<OnPlansFetched> { cancellableContinuation ->
                        continuation = cancellableContinuation
                        fetchPlans(site)
                    }
                    continuation = null
                    if (event.isError) {
                        val message = "An error occurred while fetching plans : " + event.error.message
                        appLogWrapper.e(DOMAIN_REGISTRATION, message)
                    } else if (siteId == event.site.id) {
                        result.postValue(DomainCreditAvailable(isDomainCreditAvailable(event.plans)))
                    }
                } catch (e: CancellationException) {
                    result.postValue(DomainCreditAvailable(false))
                }
            }
            return result
        } else {
            return MutableLiveData(DomainCreditAvailable(false))
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
    fun onPlansFetched(event: OnPlansFetched) = continuation?.resume(event)
}
