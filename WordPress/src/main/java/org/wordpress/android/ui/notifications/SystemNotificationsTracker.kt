package org.wordpress.android.ui.notifications

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

private const val SYSTEM_NOTIFICATIONS_ENABLED = "system_notifications_enabled"

class SystemNotificationsTracker
@Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val appPrefs: AppPrefsWrapper,
    private val notificationManager: NotificationManagerWrapper
) {
    fun checkSystemNotificationsState() {
        val previousState = appPrefs.systemNotificationsEnabled
        val notificationsEnabled = notificationManager.areNotificationsEnabled()

        if (previousState != notificationsEnabled) {
            appPrefs.systemNotificationsEnabled = notificationsEnabled
            if (notificationsEnabled) {
                analyticsTracker.track(Stat.NOTIFICATIONS_ENABLED)
            } else {
                analyticsTracker.track(Stat.NOTIFICATIONS_DISABLED)
            }
        }
    }

    fun track(stat: Stat) {
        val notificationsEnabled = notificationManager.areNotificationsEnabled()
        analyticsTracker.track(
                stat,
                mapOf(SYSTEM_NOTIFICATIONS_ENABLED to notificationsEnabled)
        )
    }
}
