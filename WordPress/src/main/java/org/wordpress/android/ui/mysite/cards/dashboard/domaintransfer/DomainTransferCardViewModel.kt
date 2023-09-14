package org.wordpress.android.ui.mysite.cards.dashboard.domaintransfer

import androidx.lifecycle.MutableLiveData
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DomainTransferCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteViewModel.State.SiteSelected
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.tabs.MySiteTabType
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

class DomainTransferCardViewModel @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    private val _refresh = MutableLiveData<Event<Boolean>>()
    val refresh = _refresh

    private val domainTransferCardVisible = AtomicReference<Boolean?>(null)
    private val waitingToTrack = AtomicBoolean(false)
    private val currentSite = AtomicReference<Int?>(null)

    fun buildDomainTransferCardParams(
        site: SiteModel,
        siteSelected: SiteSelected?
    ): DomainTransferCardBuilderParams {
        return DomainTransferCardBuilderParams(
            isEligible = shouldShowCard(site),
            onClick = { trackCardTapped(siteSelected) },
            onHideMenuItemClick = { trackCardHiddenByUser(site, siteSelected) },
            onMoreMenuClick = { trackCardMoreMenuTapped(siteSelected) }
        )
    }

    private fun shouldShowCard(
        siteModel: SiteModel,
    ): Boolean {
        return buildConfigWrapper.isJetpackApp &&
                !isCardHiddenByUser(siteModel.siteId) &&
                siteModel.isAdmin &&
                !siteModel.isWpForTeamsSite
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
            domainTransferCardVisible.set(null)
            onDashboardRefreshed(siteSelected)
        }
    }

    private fun trackCardTapped(siteSelected: SiteSelected?) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.DASHBOARD_CARD_DOMAIN_TRANSFER_TAPPED,
            mapOf(POSITION_INDEX to positionIndex(siteSelected))
        )
        _onNavigation.value = Event(SiteNavigationAction.OpenDomainTransferPage(DOMAIN_TRANSFER_PAGE_URL))
    }

    private fun trackCardMoreMenuTapped(siteSelected: SiteSelected?) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.DASHBOARD_CARD_DOMAIN_TRANSFER_MORE_MENU_TAPPED,
            mapOf(POSITION_INDEX to positionIndex(siteSelected))
        )
    }
    private fun trackCardHiddenByUser(site: SiteModel, siteSelected: SiteSelected?) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.DASHBOARD_CARD_DOMAIN_TRANSFER_HIDDEN,
            mapOf(POSITION_INDEX to positionIndex(siteSelected))
        )
        appPrefsWrapper.setShouldHideDashboardDomainTransferCard(site.siteId, true)
        _refresh.value = Event(true)
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

    private fun positionIndex(siteSelected: SiteSelected?): Int {
        return siteSelected
            ?.dashboardCardsAndItems
            ?.filterIsInstance<MySiteCardAndItem.Card.DashboardCards>()
            ?.firstOrNull()
            ?.cards
            ?.indexOfFirst {
                it is MySiteCardAndItem.Card.DashboardCards.DashboardCard.DomainTransferCardModel
            } ?: -1
    }

    private fun onDashboardRefreshed(siteSelected: SiteSelected?) {
        domainTransferCardVisible.get()?.let { isVisible ->
            if (isVisible) trackCardShown(positionIndex(siteSelected))
            waitingToTrack.set(false)
        } ?: run {
            waitingToTrack.set(true)
        }
    }

    companion object {
        const val POSITION_INDEX = "position_index"
        const val DOMAIN_TRANSFER_PAGE_URL = "https://wordpress.com/setup/google-transfer/intro"
    }
}
