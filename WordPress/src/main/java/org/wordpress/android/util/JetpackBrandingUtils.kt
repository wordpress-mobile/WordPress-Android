package org.wordpress.android.util

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.JetpackPoweredBottomSheetFeatureConfig
import javax.inject.Inject

class JetpackBrandingUtils @Inject constructor(
    private val jetpackPoweredBottomSheetFeatureConfig: JetpackPoweredBottomSheetFeatureConfig,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val siteUtilsWrapper: SiteUtilsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun shouldShowJetpackPoweredBottomSheet(): Boolean {
        return isWpComSite() && jetpackPoweredBottomSheetFeatureConfig.isEnabled() && !buildConfigWrapper.isJetpackApp
    }

    private fun isWpComSite(): Boolean {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        return selectedSite != null && siteUtilsWrapper.isAccessedViaWPComRest(selectedSite)
    }

    /**
     * Tracks
     */
    fun trackGetJetpackAppTapped() = analyticsTrackerWrapper.track(
        Stat.JETPACK_POWERED_BOTTOM_SHEET_GET_JETPACK_APP_TAPPED
    )

    fun trackDismissTapped() = analyticsTrackerWrapper.track(
        Stat.JETPACK_POWERED_BOTTOM_SHEET_CONTINUE_TAPPED
    )
}
