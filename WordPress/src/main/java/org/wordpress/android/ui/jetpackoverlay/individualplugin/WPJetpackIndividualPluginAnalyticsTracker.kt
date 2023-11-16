package org.wordpress.android.ui.jetpackoverlay.individualplugin

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class WPJetpackIndividualPluginAnalyticsTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) {
    fun trackScreenShown() = analyticsTrackerWrapper.track(
        AnalyticsTracker.Stat.WP_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY_SHOWN, emptyMap()
    )

    fun trackScreenDismissed() = analyticsTrackerWrapper.track(
        AnalyticsTracker.Stat.WP_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY_DISMISSED, emptyMap()
    )

    fun trackPrimaryButtonClick() = analyticsTrackerWrapper.track(
        AnalyticsTracker.Stat.WP_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY_PRIMARY_TAPPED, emptyMap()
    )
}
