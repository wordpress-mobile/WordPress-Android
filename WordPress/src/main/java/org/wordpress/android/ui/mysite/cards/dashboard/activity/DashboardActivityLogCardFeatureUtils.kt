package org.wordpress.android.ui.mysite.cards.dashboard.activity

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.config.DashboardCardActivityLogConfig
import javax.inject.Inject

class DashboardActivityLogCardFeatureUtils @Inject constructor(
    private val dashboardCardActivityLogConfig: DashboardCardActivityLogConfig,
    private val siteUtilsWrapper: SiteUtilsWrapper,
    private val appPrefsWrapper: AppPrefsWrapper
) {
    fun shouldRequestActivityCard(selectedSite: SiteModel): Boolean {
        if (!dashboardCardActivityLogConfig.isEnabled() || isActivityCardHiddenByUser(selectedSite.siteId)) return false
        val isWpComOrJetpack = siteUtilsWrapper.isAccessedViaWPComRest(selectedSite) ||
                selectedSite.isJetpackConnected
        return selectedSite.hasCapabilityManageOptions
                && isWpComOrJetpack
                && !selectedSite.isWpForTeamsSite
    }

    private fun isActivityCardHiddenByUser(siteId: Long): Boolean {
        return appPrefsWrapper.getShouldHideActivityDashboardCard(siteId)
    }
}
