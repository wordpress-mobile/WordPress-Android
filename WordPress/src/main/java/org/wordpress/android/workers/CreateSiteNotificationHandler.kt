package org.wordpress.android.workers

import android.content.Context
import android.content.Intent
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.push.NotificationType.CREATE_SITE
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import javax.inject.Inject

class CreateSiteNotificationHandler @Inject constructor(
    private val accountStore: AccountStore,
    private val siteStore: SiteStore,
    private val notificationsTracker: SystemNotificationsTracker
) : LocalNotificationHandler {
    override fun shouldShowNotification(): Boolean {
        return accountStore.hasAccessToken() && !siteStore.hasSite()
    }

    override fun buildIntent(context: Context): Intent {
        return ActivityLauncher.createMainActivityAndSiteCreationActivityIntent(context, CREATE_SITE)
    }

    override fun onNotificationShown() {
        notificationsTracker.trackShownNotification(CREATE_SITE)
    }
}
