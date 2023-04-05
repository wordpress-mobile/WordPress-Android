package org.wordpress.android.ui.domains

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.DashboardCardDomainFeatureConfig
import javax.inject.Inject

class DashboardCardDomainUtils @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val dashboardCardDomainFeatureConfig: DashboardCardDomainFeatureConfig,
    private val buildConfigWrapper: BuildConfigWrapper,
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

    private fun isDashboardCardDomainHiddenByUser(siteId: Long): Boolean {
        return appPrefsWrapper.getShouldHideDashboardDomainCard(siteId)
    }

    private fun isDashboardCardDomainEnabled(): Boolean {
        return buildConfigWrapper.isJetpackApp &&
                dashboardCardDomainFeatureConfig.isEnabled()
    }
}
