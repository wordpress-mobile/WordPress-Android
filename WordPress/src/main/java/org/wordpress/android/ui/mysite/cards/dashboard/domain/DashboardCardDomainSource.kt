package org.wordpress.android.ui.mysite.cards.dashboard.domain

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.MySiteSource
import org.wordpress.android.ui.mysite.MySiteUiState
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Named

const val REFRESH_DELAY = 500L

class DashboardCardDomainSource @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val siteStore: SiteStore,
    private val domainUtils: DashboardCardDomainUtils,
    private val appLogWrapper: AppLogWrapper
) : MySiteSource.MySiteRefreshSource<MySiteUiState.PartialState.CustomDomainsAvailable> {
    override val refresh = MutableLiveData(false)

    override fun build(
        coroutineScope: CoroutineScope,
        siteLocalId: Int
    ): LiveData<MySiteUiState.PartialState.CustomDomainsAvailable> {
        val data = MediatorLiveData<MySiteUiState.PartialState.CustomDomainsAvailable>()
        data.addSource(refresh) { data.refreshData(coroutineScope, siteLocalId, refresh.value) }
        refresh()
        return data
    }

    private fun shouldFetchDomains(siteLocalId: Int): Boolean {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        if (selectedSite != null && selectedSite.id == siteLocalId) {
            // By assuming that "isDomainCreditAvailable" and "hasSiteDomain" are false, we are checking other cases for
            // "shouldShowCard". If "shouldShowCard" still returns false, then we do not need to fetch domains.
            val isDomainCreditAvailable = false
            val hasSiteDomain = false

            return domainUtils.shouldShowCard(selectedSite, isDomainCreditAvailable, hasSiteDomain)
        }
        return false
    }

    private fun MediatorLiveData<MySiteUiState.PartialState.CustomDomainsAvailable>.refreshData(
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

    private fun MediatorLiveData<MySiteUiState.PartialState.CustomDomainsAvailable>.refreshData(
        coroutineScope: CoroutineScope,
        siteLocalId: Int,
        selectedSite: SiteModel?
    ) {
        if (selectedSite == null || selectedSite.id != siteLocalId || !shouldFetchDomains(siteLocalId)) {
            postState(buildErrorState())
        } else {
            fetchDomainsAndRefreshData(coroutineScope, selectedSite)
        }
    }

    private fun MediatorLiveData<MySiteUiState.PartialState.CustomDomainsAvailable>.fetchDomainsAndRefreshData(
        coroutineScope: CoroutineScope,
        selectedSite: SiteModel
    ) {
        coroutineScope.launch(bgDispatcher) {
            delay(REFRESH_DELAY) // This is necessary to wait response of "getDomainsAndPost()"
            val result = siteStore.fetchSiteDomains(selectedSite)
            val domains = result.domains
            val error = result.error

            if (result.isError) {
                appLogWrapper.e(
                    AppLog.T.DOMAIN_REGISTRATION,
                    "An error occurred while fetching domains: ${error.message}"
                )
            }

            postState(MySiteUiState.PartialState.CustomDomainsAvailable(domainUtils.hasCustomDomain(domains)))
        }
    }

    private fun buildErrorState() = MySiteUiState.PartialState.CustomDomainsAvailable(null)
}
