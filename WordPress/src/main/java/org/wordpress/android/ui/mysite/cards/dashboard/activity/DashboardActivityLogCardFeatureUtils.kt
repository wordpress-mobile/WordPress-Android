package org.wordpress.android.ui.mysite.cards.dashboard.activity

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.config.DashboardCardActivityLogConfig
import javax.inject.Inject

class DashboardActivityLogCardFeatureUtils @Inject constructor(
    private val dashboardCardActivityLogConfig: DashboardCardActivityLogConfig,
    private val siteUtilsWrapper: SiteUtilsWrapper
) {
    fun shouldRequestActivityCard(selectedSite: SiteModel): Boolean {
        if (!dashboardCardActivityLogConfig.isEnabled()) return false
        val isWpComOrJetpack = siteUtilsWrapper.isAccessedViaWPComRest(selectedSite) ||
                selectedSite.isJetpackConnected
        return selectedSite.hasCapabilityManageOptions
                && isWpComOrJetpack
                && !selectedSite.isWpForTeamsSite
    }
}
