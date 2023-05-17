package org.wordpress.android.ui.mysite.cards.dashboard.plans

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.DomainModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.asDomainModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.MySiteSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CustomDomainsAvailable
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Named

const val REFRESH_DELAY = 500L

class PlansCardDomainSource @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val siteStore: SiteStore,
    private val plansCardUtils: PlansCardUtils,
    private val appLogWrapper: AppLogWrapper
) : MySiteSource.MySiteRefreshSource<CustomDomainsAvailable> {
    override val refresh = MutableLiveData(false)

    override fun build(
        coroutineScope: CoroutineScope,
        siteLocalId: Int
    ): LiveData<CustomDomainsAvailable> {
        val result = MediatorLiveData<CustomDomainsAvailable>()
        if (shouldFetchDomains(siteLocalId)) {
            result.getDomainsAndPost(coroutineScope, siteLocalId)
        }
        result.addSource(refresh) { result.refreshData(coroutineScope, siteLocalId, refresh.value) }
        refresh()
        return result
    }

    private fun MediatorLiveData<CustomDomainsAvailable>.getDomainsAndPost(
        coroutineScope: CoroutineScope,
        siteLocalId: Int
    ) {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        if (selectedSite != null && selectedSite.id == siteLocalId) {
            coroutineScope.launch(bgDispatcher) {
                siteStore.getSiteDomains(siteLocalId)
                    .map {
                        if (it.isEmpty()) {
                            buildErrorState()
                        } else {
                            buildState(it)
                        }
                    }
                    .collect { result -> postValue(result) }
            }
        } else {
            postState(buildErrorState())
        }
    }

    private fun shouldFetchDomains(siteLocalId: Int): Boolean {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        if (selectedSite != null && selectedSite.id == siteLocalId) {
            // By assuming that "isDomainCreditAvailable" and "hasSiteDomain" are false, we are checking other cases for
            // "shouldShowCard". If "shouldShowCard" still returns false, then we do not need to fetch domains.
            val isDomainCreditAvailable = false
            val hasSiteDomain = false

            return plansCardUtils.shouldShowCard(selectedSite, isDomainCreditAvailable, hasSiteDomain)
        }
        return false
    }

    private fun MediatorLiveData<CustomDomainsAvailable>.refreshData(
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

    private fun MediatorLiveData<CustomDomainsAvailable>.refreshData(
        coroutineScope: CoroutineScope,
        siteLocalId: Int,
        selectedSite: SiteModel?
    ) {
        if (selectedSite == null || selectedSite.id != siteLocalId || !shouldFetchDomains(siteLocalId)) {
            postState(buildErrorState())
        } else {
            fetchDomainsAndPost(coroutineScope, selectedSite)
        }
    }

    private fun MediatorLiveData<CustomDomainsAvailable>.fetchDomainsAndPost(
        coroutineScope: CoroutineScope,
        selectedSite: SiteModel
    ) {
        coroutineScope.launch(bgDispatcher) {
            delay(REFRESH_DELAY) // This is necessary to wait response of "getDomainsAndPost()"
            val result = siteStore.fetchSiteDomains(selectedSite)
            val domains = result.domains?.map { it.asDomainModel() }

            if (result.isError) {
                appLogWrapper.e(
                    AppLog.T.DOMAIN_REGISTRATION,
                    "An error occurred while fetching domains: ${result.error.message}"
                )
                // If there is error, don't post any state and trust getDomainsAndPost()
            } else {
                postState(buildState(domains))
            }
        }
    }

    private fun buildState(domainModels: List<DomainModel>?) = if (domainModels.isNullOrEmpty()) {
        CustomDomainsAvailable(false)
    } else {
        CustomDomainsAvailable(plansCardUtils.hasCustomDomain(domainModels))
    }

    private fun buildErrorState() = CustomDomainsAvailable(null)
}
