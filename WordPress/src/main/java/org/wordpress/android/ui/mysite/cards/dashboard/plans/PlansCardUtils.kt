package org.wordpress.android.ui.mysite.cards.dashboard.plans

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class PlansCardUtils @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    private val cardsShownTracked = mutableListOf<MySiteCardAndItem.Type>()

    fun shouldShowCard(
        siteModel: SiteModel,
    ): Boolean {
        return buildConfigWrapper.isJetpackApp &&
                !isCardHiddenByUser(siteModel.siteId) &&
                (siteModel.isWPCom || siteModel.isWPComAtomic) &&
                siteModel.hasFreePlan &&
                siteModel.isAdmin &&
                !siteModel.isWpForTeamsSite
    }

    fun hideCard(siteId: Long) {
        appPrefsWrapper.setShouldHideDashboardPlansCard(siteId, true)
    }

    fun trackCardTapped() {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.DASHBOARD_CARD_PLANS_TAPPED
        )
    }

    fun trackCardMoreMenuTapped() {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.DASHBOARD_CARD_PLANS_MORE_MENU_TAPPED
        )
    }

    fun trackCardHiddenByUser() {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.DASHBOARD_CARD_PLANS_HIDDEN
        )
    }

    fun trackCardShown(positionIndex: Int) {
        val cardsShownTrackedPair = MySiteCardAndItem.Type.DASHBOARD_PLANS_CARD
        if (!cardsShownTracked.contains(cardsShownTrackedPair)) {
            cardsShownTracked.add(cardsShownTrackedPair)
            analyticsTrackerWrapper.track(
                AnalyticsTracker.Stat.DASHBOARD_CARD_PLANS_SHOWN,
                mapOf(POSITION_INDEX to positionIndex)
            )
        }
    }

    fun resetShown(){
        cardsShownTracked.clear()
    }

    private fun isCardHiddenByUser(siteId: Long): Boolean {
        return appPrefsWrapper.getShouldHideDashboardPlansCard(siteId)
    }


    companion object {
        const val POSITION_INDEX = "position_index"
    }
}
