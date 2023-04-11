package org.wordpress.android.ui.mysite.cards.dashboard.domain

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.DashboardCardDomainFeatureConfig
import javax.inject.Inject

class DashboardCardDomainUtils @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val dashboardCardDomainFeatureConfig: DashboardCardDomainFeatureConfig,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) {
    fun shouldShowCard(siteModel: SiteModel, isDomainCreditAvailable: Boolean): Boolean {
        return isDashboardCardDomainEnabled() &&
                !isDashboardCardDomainHiddenByUser(siteModel.siteId) &&
                (siteModel.isWPCom || siteModel.isWPComAtomic) &&
                siteModel.isAdmin &&
//                !hasSiteDomains(siteModel) &&  // this may need a separate api call!
                !isDomainCreditAvailable
    }

    fun hideCard(siteId: Long) {
        appPrefsWrapper.setShouldHideDashboardDomainCard(siteId, true)
    }

    fun trackDashboardCardDomainShown(positionIndex: Int) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.DASHBOARD_CARD_DOMAIN_SHOWN,
            mapOf(POSITION_INDEX to positionIndex)
        )
    }

    fun trackDashboardCardDomainTapped(positionIndex: Int) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.DASHBOARD_CARD_DOMAIN_TAPPED,
            mapOf(POSITION_INDEX to positionIndex)
        )
    }

    fun trackDashboardCardDomainMoreMenuTapped(positionIndex: Int) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.DASHBOARD_CARD_DOMAIN_MORE_MENU_TAPPED,
            mapOf(POSITION_INDEX to positionIndex)
        )
    }
    fun trackDashboardCardDomainHiddenByUser(positionIndex: Int) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.DASHBOARD_CARD_DOMAIN_HIDDEN,
            mapOf(POSITION_INDEX to positionIndex)
        )
    }

    private fun isDashboardCardDomainHiddenByUser(siteId: Long): Boolean {
        return appPrefsWrapper.getShouldHideDashboardDomainCard(siteId)
    }

    private fun isDashboardCardDomainEnabled(): Boolean {
        return buildConfigWrapper.isJetpackApp &&
                dashboardCardDomainFeatureConfig.isEnabled()
    }

    companion object {
        const val POSITION_INDEX = "position_index"
    }
}
