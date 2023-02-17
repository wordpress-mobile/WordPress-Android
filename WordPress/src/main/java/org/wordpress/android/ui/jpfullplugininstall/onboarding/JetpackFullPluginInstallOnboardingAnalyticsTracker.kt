package org.wordpress.android.ui.jpfullplugininstall.onboarding

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class JetpackFullPluginInstallOnboardingAnalyticsTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) {
    fun trackScreenShown() = analyticsTrackerWrapper.track(
        AnalyticsTracker.Stat.JETPACK_FULL_PLUGIN_INSTALL_ONBOARDING_SCREEN_SHOWN, emptyMap()
    )

    fun trackScreenDismissed() = analyticsTrackerWrapper.track(
        AnalyticsTracker.Stat.JETPACK_FULL_PLUGIN_INSTALL_ONBOARDING_SCREEN_DISMISSED, emptyMap()
    )

    fun trackInstallButtonClick() = analyticsTrackerWrapper.track(
        AnalyticsTracker.Stat.JETPACK_FULL_PLUGIN_INSTALL_ONBOARDING_INSTALL_TAPPED, emptyMap()
    )
}
