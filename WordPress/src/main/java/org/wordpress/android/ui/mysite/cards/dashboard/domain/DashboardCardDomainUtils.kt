package org.wordpress.android.ui.mysite.cards.dashboard.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.DomainModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.DashboardDomainCard
import org.wordpress.android.ui.mysite.MySiteViewModel.State.SiteSelected
import org.wordpress.android.ui.mysite.tabs.MySiteTabType
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.DashboardCardDomainFeatureConfig
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Named

class DashboardCardDomainUtils @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val dashboardCardDomainFeatureConfig: DashboardCardDomainFeatureConfig,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private var dashboardUpdateDebounceJob: Job? = null

    private val domainCardVisible = AtomicReference<Boolean?>(null)
    private val waitingToTrack = AtomicBoolean(false)
    private val currentSite = AtomicReference<Int?>(null)

    fun shouldShowCard(
        siteModel: SiteModel,
        isDomainCreditAvailable: Boolean,
        hasSiteCustomDomains: Boolean?
    ): Boolean {
        return isDashboardCardDomainEnabled() &&
                !isDashboardCardDomainHiddenByUser(siteModel.siteId) &&
                (siteModel.isWPCom || siteModel.isWPComAtomic) &&
                siteModel.isAdmin &&
                !siteModel.isWpForTeamsSite &&
                hasSiteCustomDomains == false &&
                !isDomainCreditAvailable
    }

    fun hideCard(siteId: Long) {
        appPrefsWrapper.setShouldHideDashboardDomainCard(siteId, true)
    }

    fun trackDashboardCardDomainShown(scope: CoroutineScope, siteSelected: SiteSelected?) {
        // cancel any existing job (debouncing mechanism)
        dashboardUpdateDebounceJob?.cancel()

        dashboardUpdateDebounceJob = scope.launch(bgDispatcher) {
            val isVisible = siteSelected
                ?.dashboardCardsAndItems
                ?.filterIsInstance<DashboardCards>()
                ?.firstOrNull()
                ?.cards
                ?.any {
                        card -> card is DashboardDomainCard
                } ?: false

            // add a delay (debouncing mechanism)
            delay(CARD_VISIBLE_DEBOUNCE)

            domainCardVisible.set(isVisible)
            if (isVisible && waitingToTrack.getAndSet(false)) {
                trackDomainCardShown(positionIndex(siteSelected))
            }
        }.also {
            it.invokeOnCompletion { cause ->
                // only set the job to null if it wasn't cancelled since cancellation is part of debouncing
                if (cause == null) dashboardUpdateDebounceJob = null
            }
        }
    }

    fun onResume(currentTab: MySiteTabType, siteSelected: SiteSelected?) {
        if (currentTab == MySiteTabType.DASHBOARD) {
            onDashboardRefreshed(siteSelected)
        } else {
            // moved away from dashboard, no longer waiting to track
            waitingToTrack.set(false)
        }
    }

    fun onSiteChanged(siteId: Int?, siteSelected: SiteSelected?) {
        if (currentSite.getAndSet(siteId) != siteId) {
            domainCardVisible.set(null)
            onDashboardRefreshed(siteSelected)
        }
    }

    fun trackDashboardCardDomainTapped(siteSelected: SiteSelected?) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.DASHBOARD_CARD_DOMAIN_TAPPED,
            mapOf(POSITION_INDEX to positionIndex(siteSelected))
        )
    }

    fun trackDashboardCardDomainMoreMenuTapped(siteSelected: SiteSelected?) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.DASHBOARD_CARD_DOMAIN_MORE_MENU_TAPPED,
            mapOf(POSITION_INDEX to positionIndex(siteSelected))
        )
    }
    fun trackDashboardCardDomainHiddenByUser(siteSelected: SiteSelected?) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.DASHBOARD_CARD_DOMAIN_HIDDEN,
            mapOf(POSITION_INDEX to positionIndex(siteSelected))
        )
    }

    private fun trackDomainCardShown(positionIndex: Int) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.DASHBOARD_CARD_DOMAIN_SHOWN,
            mapOf(POSITION_INDEX to  positionIndex)
        )
    }

    private fun isDashboardCardDomainHiddenByUser(siteId: Long): Boolean {
        return appPrefsWrapper.getShouldHideDashboardDomainCard(siteId)
    }

    private fun isDashboardCardDomainEnabled(): Boolean {
        return buildConfigWrapper.isJetpackApp &&
                dashboardCardDomainFeatureConfig.isEnabled()
    }

    private fun positionIndex(siteSelected: SiteSelected?): Int {
        return siteSelected
            ?.dashboardCardsAndItems
            ?.filterIsInstance<DashboardCards>()
            ?.firstOrNull()
            ?.cards
            ?.indexOfFirst {
                it is DashboardDomainCard
            } ?: -1
    }

    private fun onDashboardRefreshed(siteSelected: SiteSelected?) {
        domainCardVisible.get()?.let { isVisible ->
            if (isVisible) trackDomainCardShown(positionIndex(siteSelected))
            waitingToTrack.set(false)
        } ?: run {
            waitingToTrack.set(true)
        }
    }

    fun hasCustomDomain(domains: List<DomainModel>?) = domains?.any { !it.wpcomDomain } == true

    companion object {
        const val POSITION_INDEX = "position_index"
        private const val CARD_VISIBLE_DEBOUNCE = 500L
    }
}
