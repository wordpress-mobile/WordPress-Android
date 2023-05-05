package org.wordpress.android.ui.mysite.cards.dashboard.activity

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.SiteUtilsWrapper

class ActivityFeatureUtils(
    private val isDashboardCardActivityLogConfigEnabled: Boolean,
    private val siteUtilsWrapper: SiteUtilsWrapper
) {
    fun shouldRequestActivityCard(selectedSite: SiteModel): Boolean {
        if (!isDashboardCardActivityLogConfigEnabled) return false
        val isWpComOrJetpack = siteUtilsWrapper.isAccessedViaWPComRest(selectedSite) ||
                selectedSite.isJetpackConnected
        return selectedSite.hasCapabilityManageOptions
                && isWpComOrJetpack
                && !selectedSite.isWpForTeamsSite
    }
}
