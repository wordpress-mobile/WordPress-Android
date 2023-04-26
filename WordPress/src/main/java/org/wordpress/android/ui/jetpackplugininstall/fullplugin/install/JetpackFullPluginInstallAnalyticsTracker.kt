package org.wordpress.android.ui.jetpackplugininstall.fullplugin.install

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

private const val KEY_STATUS_PARAMETER = "status"
private const val KEY_DESCRIPTION_PARAMETER = "description"

class JetpackFullPluginInstallAnalyticsTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) {
    fun trackScreenShown(status: Status, description: String? = null) {
        analyticsTracker.track(
            AnalyticsTracker.Stat.JETPACK_INSTALL_FULL_PLUGIN_FLOW_VIEWED,
            mapOf(
                KEY_STATUS_PARAMETER to status.trackingValue,
                KEY_DESCRIPTION_PARAMETER to description,
            ).filterValues { it != null }
        )
    }

    fun trackCancelButtonClicked(status: Status) {
        analyticsTracker.track(
            AnalyticsTracker.Stat.JETPACK_INSTALL_FULL_PLUGIN_FLOW_CANCEL_TAPPED,
            mapOf(KEY_STATUS_PARAMETER to status.trackingValue)
        )
    }

    fun trackInstallButtonClicked() = analyticsTracker.track(
        AnalyticsTracker.Stat.JETPACK_INSTALL_FULL_PLUGIN_FLOW_INSTALL_TAPPED, emptyMap()
    )

    fun trackRetryButtonClicked() = analyticsTracker.track(
        AnalyticsTracker.Stat.JETPACK_INSTALL_FULL_PLUGIN_FLOW_RETRY_TAPPED, emptyMap()
    )

    fun trackJetpackInstallationSuccess() = analyticsTracker.track(
        AnalyticsTracker.Stat.JETPACK_INSTALL_FULL_PLUGIN_FLOW_SUCCESS, emptyMap()
    )

    fun trackDoneButtonClicked() = analyticsTracker.track(
        AnalyticsTracker.Stat.JETPACK_INSTALL_FULL_PLUGIN_FLOW_DONE_TAPPED, emptyMap()
    )

    sealed class Status(val trackingValue: String) {
        object Initial : Status("initial")
        object Loading : Status("loading")
        object Error : Status("error")
    }
}
