package org.wordpress.android.workers.notification.createsite

import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.push.NotificationType.CREATE_SITE
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.workers.notification.local.LocalNotificationHandler
import javax.inject.Inject

class CreateSiteNotificationHandler @Inject constructor(
    private val sharedPrefs: SharedPreferences,
    private val resourceProvider: ResourceProvider,
    private val accountStore: AccountStore,
    private val siteStore: SiteStore,
    private val notificationsTracker: SystemNotificationsTracker
) : LocalNotificationHandler {
    override fun shouldShowNotification(): Boolean {
        val isNotificationSettingsEnabled = sharedPrefs.getBoolean(
            resourceProvider.getString(R.string.wp_pref_notifications_main),
            true
        )
        return isNotificationSettingsEnabled && accountStore.hasAccessToken() && !siteStore.hasSite()
    }

    override fun buildFirstActionPendingIntent(context: Context, notificationId: Int): PendingIntent {
        val intent = ActivityLauncher.createMainActivityAndSiteCreationActivityIntent(
            context,
            CREATE_SITE,
            SiteCreationSource.NOTIFICATION
        )
        return PendingIntent.getActivity(
            context,
            notificationId + 1,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onNotificationShown() {
        notificationsTracker.trackShownNotification(CREATE_SITE)
    }
}
