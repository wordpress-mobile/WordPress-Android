package org.wordpress.android.workers.notification.createsite

import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.CREATE_SITE_NOTIFICATION_SCHEDULED
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.workers.notification.local.LocalNotification
import org.wordpress.android.workers.notification.local.LocalNotification.Type.CREATE_SITE
import org.wordpress.android.workers.notification.local.LocalNotificationScheduler
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CreateSiteNotificationScheduler
@Inject constructor(
    private val localNotificationScheduler: LocalNotificationScheduler,
    private val createSiteNotificationHandler: CreateSiteNotificationHandler,
    private val appsPrefs: AppPrefsWrapper,
    private val analyticsTracker: AnalyticsTrackerWrapper
) {
    fun scheduleCreateSiteNotificationIfNeeded() {
        if (createSiteNotificationHandler.shouldShowNotification() && appsPrefs.shouldScheduleCreateSiteNotification) {
            val firstNotification = LocalNotification(
                    type = CREATE_SITE,
                    delay = 1, // 1 day from now
                    delayUnits = TimeUnit.DAYS,
                    title = R.string.create_site_notification_title,
                    text = R.string.create_site_notification_text,
                    icon = R.drawable.ic_wordpress_white_24dp,
                    firstActionIcon = -1,
                    firstActionTitle = R.string.create_site_notification_create_site_action
            )
            val secondNotification = firstNotification.copy(
                    delay = 8 // 1 week after first notification
            )
            localNotificationScheduler.scheduleOneTimeNotification(firstNotification, secondNotification)

            analyticsTracker.track(CREATE_SITE_NOTIFICATION_SCHEDULED)

            appsPrefs.shouldScheduleCreateSiteNotification = false
        }
    }

    fun cancelCreateSiteNotification() {
        localNotificationScheduler.cancelScheduledNotification(CREATE_SITE)
    }
}
