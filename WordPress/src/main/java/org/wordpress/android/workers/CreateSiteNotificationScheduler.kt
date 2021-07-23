package org.wordpress.android.workers

import org.wordpress.android.R
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.workers.LocalNotification.Type.CREATE_SITE
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CreateSiteNotificationScheduler
@Inject constructor(
    private val localNotificationScheduler: LocalNotificationScheduler,
    private val createSiteNotificationHandler: CreateSiteNotificationHandler,
    private val appsPrefs: AppPrefsWrapper
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
                    actionIcon = -1,
                    actionTitle = R.string.create_site_notification_create_site_action
            )
            val secondNotification = firstNotification.copy(
                    delay = 8 // 1 week after first notification
            )
            localNotificationScheduler.scheduleOneTimeNotification(firstNotification, secondNotification)

            appsPrefs.shouldScheduleCreateSiteNotification = false
        }
    }

    fun cancelCreateSiteNotification() {
        localNotificationScheduler.cancelScheduledNotification(CREATE_SITE)
    }
}
