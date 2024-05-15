package org.wordpress.android.inappupdate

import com.google.android.play.core.install.model.AppUpdateType
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class InAppUpdateAnalyticsTracker @Inject constructor(
    private val tracker: AnalyticsTrackerWrapper
) {
    fun trackUpdateShown(updateType: Int) {
        tracker.track(AnalyticsTracker.Stat.IN_APP_UPDATE_SHOWN, createPropertyMap(updateType))
    }

    fun trackUpdateAccepted(updateType: Int) {
        tracker.track(AnalyticsTracker.Stat.IN_APP_UPDATE_ACCEPTED, createPropertyMap(updateType))
    }

    fun trackUpdateDismissed() {
        tracker.track(AnalyticsTracker.Stat.IN_APP_UPDATE_DISMISSED)
    }

    private fun createPropertyMap(updateType: Int): Map<String, String> {
        return when (updateType) {
            AppUpdateType.FLEXIBLE -> mapOf(PROPERTY_UPDATE_TYPE to UPDATE_TYPE_FLEXIBLE)
            AppUpdateType.IMMEDIATE -> mapOf(PROPERTY_UPDATE_TYPE to UPDATE_TYPE_BLOCKING)
            else -> emptyMap()
        }
    }

    companion object {
        private const val PROPERTY_UPDATE_TYPE = "type"
        private const val UPDATE_TYPE_FLEXIBLE = "flexible"
        private const val UPDATE_TYPE_BLOCKING = "blocking"
    }
}
