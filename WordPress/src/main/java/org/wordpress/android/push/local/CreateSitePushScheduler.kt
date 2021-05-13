package org.wordpress.android.push.local

import org.wordpress.android.R
import org.wordpress.android.push.local.LocalPush.Type.CREATE_SITE
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CreateSitePushScheduler
@Inject constructor(
    private val localPushScheduler: LocalPushScheduler
) {
    fun scheduleCreateSiteNotification() {
        val createSiteNotification = LocalPush(
                CREATE_SITE,
                10,
                TimeUnit.SECONDS,
                R.string.my_site_add_new_site,
                R.string.my_site_create_new_site,
                R.drawable.ic_add_white_24dp
        )
        localPushScheduler.scheduleOneTimeNotification(createSiteNotification)
    }

    fun cancelCreateSiteNotification() {
        localPushScheduler.cancelScheduledNotification(CREATE_SITE)
    }
}
