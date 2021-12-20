package org.wordpress.android.ui.mysite.cards.domainregistration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
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
import org.wordpress.android.ui.mysite.MySiteSource.MySiteRefreshSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.DomainCreditAvailable
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.plans.isDomainCreditAvailable
import org.wordpress.android.util.AppLog.T.DOMAIN_REGISTRATION
import org.wordpress.android.util.SiteUtilsWrapper
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.resume

class DomainRegistrationSource @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val dispatcher: Dispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val appLogWrapper: AppLogWrapper,
    private val siteUtils: SiteUtilsWrapper
) : MySiteRefreshSource<DomainCreditAvailable> {
    override val refresh = MutableLiveData(true)

    private val continuations = mutableMapOf<Int, CancellableContinuation<OnPlansFetched>?>()

    init {
        dispatcher.register(this)
    }

    fun clear() {
        dispatcher.unregister(this)
    }

    override fun build(coroutineScope: CoroutineScope, siteLocalId: Int): LiveData<DomainCreditAvailable> {
        val data = MediatorLiveData<DomainCreditAvailable>()
        data.refreshData(coroutineScope, siteLocalId)
        data.addSource(refresh) { data.refreshData(coroutineScope, siteLocalId, refresh.value) }
        return data
    }

    private fun MediatorLiveData<DomainCreditAvailable>.refreshData(
        coroutineScope: CoroutineScope,
        siteLocalId: Int,
        isRefresh: Boolean? = null
    ) {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        when (isRefresh) {
            null, true -> refreshData(coroutineScope, siteLocalId, selectedSite)
            false -> Unit // Do nothing
        }
    }

    private fun MediatorLiveData<DomainCreditAvailable>.refreshData(
        coroutineScope: CoroutineScope,
        siteLocalId: Int,
        selectedSite: SiteModel?
    ) {
        if (selectedSite == null || selectedSite.id != siteLocalId || !shouldFetchPlans(selectedSite)) {
            postState(DomainCreditAvailable(false))
        } else {
            fetchPlansAndRefreshData(coroutineScope, siteLocalId, selectedSite)
        }
    }

    private fun MediatorLiveData<DomainCreditAvailable>.fetchPlansAndRefreshData(
        coroutineScope: CoroutineScope,
        siteLocalId: Int,
        selectedSite: SiteModel
    ) {
        if (continuations[siteLocalId] == null) {
            coroutineScope.launch(bgDispatcher) { fetchPlans(siteLocalId, selectedSite) }
        } else {
            appLogWrapper.d(DOMAIN_REGISTRATION, "A request is already running for $siteLocalId")
        }
    }

    @Suppress("SwallowedException")
    private suspend fun MediatorLiveData<DomainCreditAvailable>.fetchPlans(siteLocalId: Int, selectedSite: SiteModel) {
        try {
            val event = suspendCancellableCoroutine<OnPlansFetched> { cancellableContinuation ->
                continuations[siteLocalId] = cancellableContinuation
                dispatchFetchPlans(selectedSite)
            }
            when {
                event.isError -> {
                    val message = "An error occurred while fetching plans :${event.error.message}"
                    appLogWrapper.e(DOMAIN_REGISTRATION, message)
                    postState(DomainCreditAvailable(false))
                }
                siteLocalId == event.site.id -> {
                    postState(DomainCreditAvailable(isDomainCreditAvailable(event.plans)))
                }
                else -> {
                    postState(DomainCreditAvailable(false))
                }
            }
        } catch (e: CancellationException) {
            postState(DomainCreditAvailable(false))
        }
    }

    private fun shouldFetchPlans(site: SiteModel) = !siteUtils.onFreePlan(site)

    private fun dispatchFetchPlans(site: SiteModel) = dispatcher.dispatch(SiteActionBuilder.newFetchPlansAction(site))

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlansFetched(event: OnPlansFetched) {
        continuations[event.site.id]?.resume(event)
        continuations[event.site.id] = null
    }
}
