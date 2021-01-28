package org.wordpress.android.ui.mysite

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore.OnPlansFetched
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.DomainCreditAvailable
import org.wordpress.android.ui.plans.isDomainCreditAvailable
import org.wordpress.android.util.AppLog.T.DOMAIN_REGISTRATION
import org.wordpress.android.util.SiteUtilsWrapper
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class DomainRegistrationHandler
@Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val appLogWrapper: AppLogWrapper,
    private val siteUtils: SiteUtilsWrapper
) : MySiteSource<DomainCreditAvailable> {
    private var continuation: CancellableContinuation<OnPlansFetched>? = null

    override fun buildSource(siteId: Int) = flow {
        continuation?.cancel()
        continuation = null
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null || site.id != siteId) {
            return@flow
        }
        if (shouldFetchPlans(site)) {
            try {
                val event = suspendCancellableCoroutine<OnPlansFetched> { cancellableContinuation ->
                    continuation = cancellableContinuation
                    fetchPlans(site)
                }
                continuation = null
                if (event.isError) {
                    appLogWrapper.e(DOMAIN_REGISTRATION, "An error occurred while fetching plans : " + event.error.message)
                } else if (siteId == event.site.id) {
                    emit(DomainCreditAvailable(isDomainCreditAvailable(event.plans)))
                }
            } catch (e: CancellationException) {
                emit(DomainCreditAvailable(false))
            }
        } else {
            emit(DomainCreditAvailable(false))
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
