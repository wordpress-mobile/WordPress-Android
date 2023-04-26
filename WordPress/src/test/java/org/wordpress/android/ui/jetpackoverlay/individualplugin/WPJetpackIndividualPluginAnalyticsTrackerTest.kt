package org.wordpress.android.ui.jetpackoverlay.individualplugin

import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

class WPJetpackIndividualPluginAnalyticsTrackerTest {
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val tracker = WPJetpackIndividualPluginAnalyticsTracker(analyticsTrackerWrapper)

    @Test
    fun `Should track screen shown when trackScreenShown is called`() {
        tracker.trackScreenShown()
        verify(analyticsTrackerWrapper).track(
            AnalyticsTracker.Stat.WP_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY_SHOWN, emptyMap()
        )
    }

    @Test
    fun `Should track screen dismissed when trackScreenDismissed is called`() {
        tracker.trackScreenDismissed()
        verify(analyticsTrackerWrapper).track(
            AnalyticsTracker.Stat.WP_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY_DISMISSED, emptyMap()
        )
    }

    @Test
    fun `Should track install button click when trackInstallButtonClick is called`() {
        tracker.trackPrimaryButtonClick()
        verify(analyticsTrackerWrapper).track(
            AnalyticsTracker.Stat.WP_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY_PRIMARY_TAPPED, emptyMap()
        )
    }
}
