package org.wordpress.android.util

import android.view.View
import android.view.View.OnScrollChangeListener
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalBrandingUtil
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
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

    @Suppress("unused")
    fun getBrandingTextForScreen(screen: Screen) = jetpackFeatureRemovalBrandingUtil.getBrandingTextByPhase(screen)

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
    fun trackBadgeTapped(screen: Screen) = analyticsTrackerWrapper.track(
        Stat.JETPACK_POWERED_BADGE_TAPPED,
        mapOf(SCREEN_KEY to screen.trackingName)
    )

    fun trackBannerTapped(screen: Screen) = analyticsTrackerWrapper.track(
        Stat.JETPACK_POWERED_BANNER_TAPPED,
        mapOf(SCREEN_KEY to screen.trackingName)
    )

    fun trackGetJetpackAppTapped() = analyticsTrackerWrapper.track(
        Stat.JETPACK_POWERED_BOTTOM_SHEET_GET_JETPACK_APP_TAPPED
    )

    fun trackDismissTapped() = analyticsTrackerWrapper.track(
        Stat.JETPACK_POWERED_BOTTOM_SHEET_CONTINUE_TAPPED
    )

    @Suppress("ClassName")
    sealed class Screen(open val trackingName: String) {

        sealed class ScreenWithDynamicBranding(
            override val trackingName: String,
            val featureName: UiString? = null,
            val isFeatureNameSingular: Boolean,
        ) : Screen(trackingName) {

            val featureVerb
                get() = when (isFeatureNameSingular) {
                    true -> UiStringRes(R.string.wp_jetpack_powered_phase_3_feature_verb_singular_is)
                    else -> UiStringRes(R.string.wp_jetpack_powered_phase_3_feature_verb_plural_are)
                }

            fun getBrandingTextParams(timeUntilDeadline: Int? = null) = listOfNotNull(
                featureName,
                featureVerb,
                timeUntilDeadline?.let { UiStringText("$it") },
            )
        }

        object APP_SETTINGS : Screen(trackingName = "app_settings")
        object HOME : Screen(trackingName = "home")
        object ME : Screen(trackingName = "me")
        object PEOPLE : Screen(trackingName = "people")
        object PERSON : Screen(trackingName = "person")

        object ACTIVITY_LOG : ScreenWithDynamicBranding(
            trackingName = "activity_log",
            featureName = UiStringRes(R.string.activity_log),
            isFeatureNameSingular = true,
        )

        object ACTIVITY_LOG_DETAIL : ScreenWithDynamicBranding(
            trackingName = "activity_log_detail",
            featureName = UiStringRes(R.string.activity_log),
            isFeatureNameSingular = true,
        )

        object BACKUP : ScreenWithDynamicBranding(
            trackingName = "backup",
            featureName = UiStringRes(R.string.backup),
            isFeatureNameSingular = true,
        )

        object NOTIFICATIONS : ScreenWithDynamicBranding(
            trackingName = "notifications",
            featureName = UiStringRes(R.string.notifications_screen_title),
            isFeatureNameSingular = false,
        )

        object NOTIFICATIONS_SETTINGS : ScreenWithDynamicBranding(
            trackingName = "notifications_settings",
            featureName = UiStringRes(R.string.notification_settings),
            isFeatureNameSingular = false,
        )

        object READER : ScreenWithDynamicBranding(
            "reader",
            featureName = UiStringRes(R.string.reader_screen_title),
            isFeatureNameSingular = true,
        )

        object READER_POST_DETAIL : ScreenWithDynamicBranding(
            "reader_post_detail",
            featureName = UiStringRes(R.string.reader_screen_title),
            isFeatureNameSingular = true,
        )

        object READER_SEARCH : ScreenWithDynamicBranding(
            "reader_search",
            featureName = UiStringRes(R.string.reader_screen_title),
            isFeatureNameSingular = true,
        )

        object SHARE : ScreenWithDynamicBranding(
            "share",
            featureName = UiStringRes(R.string.my_site_btn_sharing),
            isFeatureNameSingular = true,
        )

        object STATS : ScreenWithDynamicBranding(
            "stats",
            featureName = UiStringRes(R.string.stats),
            isFeatureNameSingular = false,
        )

        object SCAN : ScreenWithDynamicBranding(
            "scan",
            featureName = UiStringRes(R.string.scan),
            isFeatureNameSingular = true,
        )

        object THEMES : ScreenWithDynamicBranding(
            "themes",
            featureName = UiStringRes(R.string.themes),
            isFeatureNameSingular = false,
        )
    }

    companion object {
        private const val SCREEN_KEY = "screen"

    }
}
