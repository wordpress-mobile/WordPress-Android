package org.wordpress.android.ui.jpfullplugininstall.onboarding

import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

class JetpackFullPluginInstallOnboardingAnalyticsTrackerTest {
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val classToTest = JetpackFullPluginInstallOnboardingAnalyticsTracker(
        analyticsTrackerWrapper = analyticsTrackerWrapper,
    )

    @Test
    fun `Should track screen shown when trackScreenShown is called`() {
        classToTest.trackScreenShown()
        verify(analyticsTrackerWrapper).track(
            AnalyticsTracker.Stat.JETPACK_FULL_PLUGIN_INSTALL_ONBOARDING_SCREEN_SHOWN, emptyMap()
        )
    }

    @Test
    fun `Should track screen dismissed when trackScreenDismissed is called`() {
        classToTest.trackScreenDismissed()
        verify(analyticsTrackerWrapper).track(
            AnalyticsTracker.Stat.JETPACK_FULL_PLUGIN_INSTALL_ONBOARDING_SCREEN_DISMISSED, emptyMap()
        )
    }

    @Test
    fun `Should track install button click when trackInstallButtonClick is called`() {
        classToTest.trackInstallButtonClick()
        verify(analyticsTrackerWrapper).track(
            AnalyticsTracker.Stat.JETPACK_FULL_PLUGIN_INSTALL_ONBOARDING_INSTALL_TAPPED, emptyMap()
        )
    }
}
