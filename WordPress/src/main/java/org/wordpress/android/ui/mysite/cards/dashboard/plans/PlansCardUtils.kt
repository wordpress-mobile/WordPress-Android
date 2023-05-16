package org.wordpress.android.ui.mysite.cards.dashboard.plans

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.DashboardPlansCard
import org.wordpress.android.ui.mysite.MySiteViewModel
import org.wordpress.android.ui.mysite.tabs.MySiteTabType
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.DashboardCardFreeToPaidPlansFeatureConfig
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Named

class PlansCardUtils @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val plansFreeToPaidFeatureConfig: DashboardCardFreeToPaidPlansFeatureConfig,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private var dashboardUpdateDebounceJob: Job? = null

    private val plansCardVisible = AtomicReference<Boolean?>(null)
    private val waitingToTrack = AtomicBoolean(false)
    private val currentSite = AtomicReference<Int?>(null)

    fun shouldShowCard(
        siteModel: SiteModel,
        isDomainCreditAvailable: Boolean,
        hasSiteCustomDomains: Boolean?
    ): Boolean {
        return isCardEnabled() &&
                !isCardHiddenByUser(siteModel.siteId) &&
                siteModel.hasFreePlan &&
                (siteModel.isWPCom || siteModel.isWPComAtomic) &&
                siteModel.isAdmin &&
                !siteModel.isWpForTeamsSite &&
                hasSiteCustomDomains == false &&
                !isDomainCreditAvailable
    }

    fun hideCard(siteId: Long) {
        appPrefsWrapper.setShouldHideDashboardPlansCard(siteId, true)
    }

    fun trackCardShown(scope: CoroutineScope, siteSelected: MySiteViewModel.State.SiteSelected?) {
        // cancel any existing job (debouncing mechanism)
        dashboardUpdateDebounceJob?.cancel()

        dashboardUpdateDebounceJob = scope.launch(bgDispatcher) {
            val isVisible = siteSelected
                ?.dashboardCardsAndItems
                ?.filterIsInstance<MySiteCardAndItem.Card.DashboardCards>()
                ?.firstOrNull()
                ?.cards
                ?.any {
                        card -> card is DashboardPlansCard
                } ?: false

            // add a delay (debouncing mechanism)
            delay(CARD_VISIBLE_DEBOUNCE)

            plansCardVisible.set(isVisible)
            if (isVisible && waitingToTrack.getAndSet(false)) {
                trackCardShown(positionIndex(siteSelected))
            }
        }.also {
            it.invokeOnCompletion { cause ->
                // only set the job to null if it wasn't cancelled since cancellation is part of debouncing
                if (cause == null) dashboardUpdateDebounceJob = null
            }
        }
    }

    fun onResume(currentTab: MySiteTabType, siteSelected: MySiteViewModel.State.SiteSelected?) {
        if (currentTab == MySiteTabType.DASHBOARD) {
            onDashboardRefreshed(siteSelected)
        } else {
            // moved away from dashboard, no longer waiting to track
            waitingToTrack.set(false)
        }
    }

    fun onSiteChanged(siteId: Int?, siteSelected: MySiteViewModel.State.SiteSelected?) {
        if (currentSite.getAndSet(siteId) != siteId) {
            plansCardVisible.set(null)
            onDashboardRefreshed(siteSelected)
        }
    }

    fun trackCardTapped(siteSelected: MySiteViewModel.State.SiteSelected?) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.DASHBOARD_CARD_PLANS_TAPPED,
            mapOf(POSITION_INDEX to positionIndex(siteSelected))
        )
    }

    fun trackCardMoreMenuTapped(siteSelected: MySiteViewModel.State.SiteSelected?) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.DASHBOARD_CARD_PLANS_MORE_MENU_TAPPED,
            mapOf(POSITION_INDEX to positionIndex(siteSelected))
        )
    }
    fun trackCardHiddenByUser(siteSelected: MySiteViewModel.State.SiteSelected?) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.DASHBOARD_CARD_PLANS_HIDDEN,
            mapOf(POSITION_INDEX to positionIndex(siteSelected))
        )
    }

    private fun trackCardShown(positionIndex: Int) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.DASHBOARD_CARD_PLANS_SHOWN,
            mapOf(POSITION_INDEX to  positionIndex)
        )
    }

    private fun isCardHiddenByUser(siteId: Long): Boolean {
        return appPrefsWrapper.getShouldHideDashboardPlansCard(siteId)
    }

    private fun isCardEnabled(): Boolean {
        return buildConfigWrapper.isJetpackApp &&
                plansFreeToPaidFeatureConfig.isEnabled()
    }

    private fun positionIndex(siteSelected: MySiteViewModel.State.SiteSelected?): Int {
        return siteSelected
            ?.dashboardCardsAndItems
            ?.filterIsInstance<MySiteCardAndItem.Card.DashboardCards>()
            ?.firstOrNull()
            ?.cards
            ?.indexOfFirst {
                it is DashboardPlansCard
            } ?: -1
    }

    private fun onDashboardRefreshed(siteSelected: MySiteViewModel.State.SiteSelected?) {
        plansCardVisible.get()?.let { isVisible ->
            if (isVisible) trackCardShown(positionIndex(siteSelected))
            waitingToTrack.set(false)
        } ?: run {
            waitingToTrack.set(true)
        }
    }

    companion object {
        const val POSITION_INDEX = "position_index"
        private const val CARD_VISIBLE_DEBOUNCE = 500L
    }
}
