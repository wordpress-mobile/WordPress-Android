package org.wordpress.android.util

import android.view.View
import android.view.View.OnScrollChangeListener
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalBrandingUtil
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.JetpackPoweredBottomSheetFeatureConfig
import org.wordpress.android.util.config.JetpackPoweredFeatureConfig
import javax.inject.Inject

class JetpackBrandingUtils @Inject constructor(
    private val jetpackPoweredFeatureConfig: JetpackPoweredFeatureConfig,
    private val jetpackPoweredBottomSheetFeatureConfig: JetpackPoweredBottomSheetFeatureConfig,
    private val jetpackFeatureRemovalBrandingUtil: JetpackFeatureRemovalBrandingUtil,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val siteUtilsWrapper: SiteUtilsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun shouldShowJetpackBranding(): Boolean {
        return isWpComSite() && jetpackPoweredFeatureConfig.isEnabled() && !buildConfigWrapper.isJetpackApp
                && !jetpackFeatureRemovalBrandingUtil.isInRemovalPhase()
    }

    fun shouldShowJetpackBrandingForPhaseOne(): Boolean {
        return shouldShowJetpackBranding() && jetpackFeatureRemovalBrandingUtil.shouldShowPhaseOneBranding()
    }

    fun shouldShowJetpackBrandingForPhaseTwo(): Boolean {
        return shouldShowJetpackBranding() && jetpackFeatureRemovalBrandingUtil.shouldShowPhaseTwoBranding()
    }

    fun shouldShowJetpackPoweredBottomSheet(): Boolean {
        return isWpComSite() && jetpackPoweredBottomSheetFeatureConfig.isEnabled() && !buildConfigWrapper.isJetpackApp
    }

    fun getBrandingTextForScreen(screen: JetpackPoweredScreen): UiString {
        return jetpackFeatureRemovalBrandingUtil.getBrandingTextByPhase(screen)
    }

    fun showJetpackBannerIfScrolledToTop(banner: View, scrollableView: RecyclerView) {
        banner.isVisible = true

        val isEmpty = scrollableView.layoutManager?.itemCount == 0
        val scrollOffset = scrollableView.computeVerticalScrollOffset()

        banner.translationY = if (scrollOffset == 0 || isEmpty) {
            // Show
            0f
        } else {
            // Hide by moving down
            banner.resources.getDimension(R.dimen.jetpack_banner_height)
        }
    }

    fun initJetpackBannerAnimation(banner: View, scrollableView: RecyclerView) {
        scrollableView.setOnScrollChangeListener(object : OnScrollChangeListener {
            private var isScrollAtTop = true

            override fun onScrollChange(v: View?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
                val scrollOffset = scrollableView.computeVerticalScrollOffset()

                if (scrollOffset == 0 && !isScrollAtTop) {
                    // Show the banner by moving up
                    isScrollAtTop = true
                    banner.animate().translationY(0f).start()
                } else if (scrollOffset != 0 && isScrollAtTop) {
                    // Hide the banner by moving down
                    isScrollAtTop = false
                    val jetpackBannerHeight = banner.resources.getDimension(R.dimen.jetpack_banner_height)
                    banner.animate().translationY(jetpackBannerHeight).start()
                }
            }
        })
    }

    private fun isWpComSite(): Boolean {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        return selectedSite != null && siteUtilsWrapper.isAccessedViaWPComRest(selectedSite)
    }

    /**
     * Tracks
     */
    fun trackBadgeTapped(screen: JetpackPoweredScreen) = analyticsTrackerWrapper.track(
        Stat.JETPACK_POWERED_BADGE_TAPPED,
        mapOf(SCREEN_KEY to screen.trackingName)
    )

    fun trackBannerTapped(screen: JetpackPoweredScreen) = analyticsTrackerWrapper.track(
        Stat.JETPACK_POWERED_BANNER_TAPPED,
        mapOf(SCREEN_KEY to screen.trackingName)
    )

    fun trackGetJetpackAppTapped() = analyticsTrackerWrapper.track(
        Stat.JETPACK_POWERED_BOTTOM_SHEET_GET_JETPACK_APP_TAPPED
    )

    fun trackDismissTapped() = analyticsTrackerWrapper.track(
        Stat.JETPACK_POWERED_BOTTOM_SHEET_CONTINUE_TAPPED
    )

    companion object {
        private const val SCREEN_KEY = "screen"
    }
}
