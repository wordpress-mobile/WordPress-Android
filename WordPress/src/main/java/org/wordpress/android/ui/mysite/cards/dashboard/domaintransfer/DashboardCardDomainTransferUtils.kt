package org.wordpress.android.ui.mysite.cards.dashboard.domaintransfer

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.DomainTransferCardModel
import org.wordpress.android.ui.mysite.MySiteViewModel
import org.wordpress.android.ui.mysite.tabs.MySiteTabType
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Named

class DashboardCardDomainTransferUtils @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private var dashboardUpdateDebounceJob: Job? = null

    private val domainTransferCardVisible = AtomicReference<Boolean?>(null)
    private val waitingToTrack = AtomicBoolean(false)
    private val currentSite = AtomicReference<Int?>(null)

    fun shouldShowCard(
        siteModel: SiteModel,
    ): Boolean {
        return buildConfigWrapper.isJetpackApp &&
                !isCardHiddenByUser(siteModel.siteId) &&
                siteModel.isAdmin &&
                !siteModel.isWpForTeamsSite
    }

    fun hideCard(siteId: Long) {
        appPrefsWrapper.setShouldHideDashboardDomainTransferCard(siteId, true)
    }

    fun trackCardShown(scope: CoroutineScope, siteSelected: MySiteViewModel.State.SiteSelected?) {
        // cancel any existing job (debouncing mechanism)
        dashboardUpdateDebounceJob?.cancel()

        dashboardUpdateDebounceJob = scope.launch(bgDispatcher) {
            val isVisible = siteSelected
                ?.dashboardCardsAndItems
                ?.filterIsInstance<DashboardCards>()
                ?.firstOrNull()
                ?.cards
                ?.any {
                        card -> card is DomainTransferCardModel
                } ?: false

            // add a delay (debouncing mechanism)
            delay(CARD_VISIBLE_DEBOUNCE)

            domainTransferCardVisible.set(isVisible)
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
            domainTransferCardVisible.set(null)
            onDashboardRefreshed(siteSelected)
        }
    }

    fun trackCardTapped(siteSelected: MySiteViewModel.State.SiteSelected?) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.DASHBOARD_CARD_DOMAIN_TRANSFER_TAPPED,
            mapOf(POSITION_INDEX to positionIndex(siteSelected))
        )
    }

    fun trackCardMoreMenuTapped(siteSelected: MySiteViewModel.State.SiteSelected?) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.DASHBOARD_CARD_DOMAIN_TRANSFER_MORE_MENU_TAPPED,
            mapOf(POSITION_INDEX to positionIndex(siteSelected))
        )
    }
    fun trackCardHiddenByUser(siteSelected: MySiteViewModel.State.SiteSelected?) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.DASHBOARD_CARD_DOMAIN_TRANSFER_HIDDEN,
            mapOf(POSITION_INDEX to positionIndex(siteSelected))
        )
    }

    private fun trackCardShown(positionIndex: Int) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.DASHBOARD_CARD_DOMAIN_TRANSFER_SHOWN,
            mapOf(POSITION_INDEX to  positionIndex)
        )
    }

    private fun isCardHiddenByUser(siteId: Long): Boolean {
        return appPrefsWrapper.getShouldHideDashboardDomainTransferCard(siteId)
    }

    private fun positionIndex(siteSelected: MySiteViewModel.State.SiteSelected?): Int {
        return siteSelected
            ?.dashboardCardsAndItems
            ?.filterIsInstance<DashboardCards>()
            ?.firstOrNull()
            ?.cards
            ?.indexOfFirst {
                it is DomainTransferCardModel
            } ?: -1
    }

    private fun onDashboardRefreshed(siteSelected: MySiteViewModel.State.SiteSelected?) {
        domainTransferCardVisible.get()?.let { isVisible ->
            if (isVisible) trackCardShown(positionIndex(siteSelected))
            waitingToTrack.set(false)
        } ?: run {
            waitingToTrack.set(true)
        }
    }

    companion object {
        const val POSITION_INDEX = "position_index"
        private const val CARD_VISIBLE_DEBOUNCE = 500L
        const val DOMAIN_TRANSFER_PAGE_URL = "https://wordpress.com/setup/google-transfer/intro"
    }
}
