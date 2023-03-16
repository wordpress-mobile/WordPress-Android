package org.wordpress.android.ui.jpfullplugininstall.install

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class JetpackFullPluginInstallAnalyticsTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) {
    fun trackScreenShown(status: Status) {
        analyticsTracker.track(
            Stat.JETPACK_INSTALL_FULL_PLUGIN_FLOW_VIEWED,
            mapOf(KEY_STATUS_PARAMETER to status.trackingValue)
        )
    }

    fun trackCancelButtonClicked(status: Status) {
        analyticsTracker.track(
            Stat.JETPACK_INSTALL_FULL_PLUGIN_FLOW_CANCEL_TAPPED,
            mapOf(KEY_STATUS_PARAMETER to status.trackingValue)
        )
    }

    fun trackInstallButtonClicked() = analyticsTracker.track(
        Stat.JETPACK_INSTALL_FULL_PLUGIN_FLOW_INSTALL_TAPPED, emptyMap()
    )

    fun trackRetryButtonClicked() = analyticsTracker.track(
        Stat.JETPACK_INSTALL_FULL_PLUGIN_FLOW_RETRY_TAPPED, emptyMap()
    )

    fun trackJetpackInstallationSuccess() = analyticsTracker.track(
        Stat.JETPACK_INSTALL_FULL_PLUGIN_FLOW_SUCCESS, emptyMap()
    )

    fun trackDoneButtonClicked() = analyticsTracker.track(
        Stat.JETPACK_INSTALL_FULL_PLUGIN_FLOW_DONE_TAPPED, emptyMap()
    )

    sealed class Status(val trackingValue: String) {
        object Initial : Status("initial")
        object Loading : Status("loading")
        object Error : Status("error")
    }
}

private const val KEY_STATUS_PARAMETER = "status"
