package org.wordpress.android.workers

import org.wordpress.android.R
import org.wordpress.android.workers.LocalNotification.Type.CREATE_SITE
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CreateSiteNotificationScheduler
@Inject constructor(
    private val localNotificationScheduler: LocalNotificationScheduler
) {
    fun scheduleCreateSiteNotification() {
        val createSiteNotification = LocalNotification(
                type = CREATE_SITE,
                delay = 10,
                delayUnits = TimeUnit.SECONDS,
                title = R.string.create_site_notification_title,
                text = R.string.create_site_notification_text,
                icon = R.drawable.ic_wordpress_white_24dp,
                actionIcon = -1,
                actionTitle = R.string.create_site_notification_create_site_action
        )
        localNotificationScheduler.scheduleOneTimeNotification(createSiteNotification)
    }

    fun cancelCreateSiteNotification() {
        localNotificationScheduler.cancelScheduledNotification(CREATE_SITE)
    }
}
