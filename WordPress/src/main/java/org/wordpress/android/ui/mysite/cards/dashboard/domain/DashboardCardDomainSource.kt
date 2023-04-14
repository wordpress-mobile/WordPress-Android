package org.wordpress.android.ui.mysite.cards.dashboard.domain

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
import org.wordpress.android.fluxc.store.SiteStore.FetchedDomainsPayload
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.MySiteSource
import org.wordpress.android.ui.mysite.MySiteUiState
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.plans.hasSiteDomains
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.resume

class DashboardCardDomainSource @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val dispatcher: Dispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val appLogWrapper: AppLogWrapper
) : MySiteSource.MySiteRefreshSource<MySiteUiState.PartialState.DomainsAvailable> {
    override val refresh = MutableLiveData(false)

    private val continuations = mutableMapOf<Int, CancellableContinuation<FetchedDomainsPayload>?>()

    init {
        dispatcher.register(this)
    }

    fun clear() {
        dispatcher.unregister(this)
    }

    override fun build(
        coroutineScope: CoroutineScope,
        siteLocalId: Int
    ): LiveData<MySiteUiState.PartialState.DomainsAvailable> {
        val data = MediatorLiveData<MySiteUiState.PartialState.DomainsAvailable>()
        data.addSource(refresh) { data.refreshData(coroutineScope, siteLocalId, refresh.value) }
        refresh()
        return data
    }

    private fun MediatorLiveData<MySiteUiState.PartialState.DomainsAvailable>.refreshData(
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

    private fun MediatorLiveData<MySiteUiState.PartialState.DomainsAvailable>.refreshData(
        coroutineScope: CoroutineScope,
        siteLocalId: Int,
        selectedSite: SiteModel?
    ) {
        if (selectedSite == null || selectedSite.id != siteLocalId) {
            postState(MySiteUiState.PartialState.DomainsAvailable(false))
        } else {
            fetchDomainsAndRefreshData(coroutineScope, siteLocalId, selectedSite)
        }
    }

    private fun MediatorLiveData<MySiteUiState.PartialState.DomainsAvailable>.fetchDomainsAndRefreshData(
        coroutineScope: CoroutineScope,
        siteLocalId: Int,
        selectedSite: SiteModel
    ) {
        if (continuations[siteLocalId] == null) {
            coroutineScope.launch(bgDispatcher) { fetchDomains(siteLocalId, selectedSite) }
        } else {
            appLogWrapper.d(AppLog.T.DOMAIN_REGISTRATION, "A request is already running for $siteLocalId")
        }
    }

    @Suppress("SwallowedException")
    private suspend fun MediatorLiveData<MySiteUiState.PartialState.DomainsAvailable>.fetchDomains(
        siteLocalId: Int,
        selectedSite: SiteModel
    ) {
        try {
            val event = suspendCancellableCoroutine<FetchedDomainsPayload> { cancellableContinuation ->
                continuations[siteLocalId] = cancellableContinuation
                dispatchFetchDomains(selectedSite)
            }
            when {
                event.isError -> {
                    val message = "An error occurred while fetching plans :${event.error.message}"
                    appLogWrapper.e(AppLog.T.DOMAIN_REGISTRATION, message)
                    postState(MySiteUiState.PartialState.DomainsAvailable(false))
                }
                siteLocalId == event.site.id -> {
                    postState(MySiteUiState.PartialState.DomainsAvailable(hasSiteDomains(event.domains)))
                }
                else -> {
                    postState(MySiteUiState.PartialState.DomainsAvailable(false))
                }
            }
        } catch (e: CancellationException) {
            postState(MySiteUiState.PartialState.DomainsAvailable(false))
        }
    }

    private fun dispatchFetchDomains(site: SiteModel) = dispatcher.dispatch(SiteActionBuilder.newFetchPlansAction(site))

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDomainsFetched(event: FetchedDomainsPayload) {
        continuations[event.site.id]?.resume(event)
        continuations[event.site.id] = null
    }
}
