package org.wordpress.android.workers.weeklystatsroundup

import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import org.wordpress.android.R.color
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.push.NotificationPushIds
import org.wordpress.android.push.NotificationType.BLOGGING_REMINDERS
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAnalyticsTracker
import org.wordpress.android.ui.posts.PostListType.DRAFTS
import org.wordpress.android.ui.posts.PostsListActivity
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class WeeklyStatsRoundupNotifier @Inject constructor(
    val contextProvider: ContextProvider,
    val resourceProvider: ResourceProvider,
    val siteStore: SiteStore,
    val accountStore: AccountStore,
    val weeklyStatsRoundupNotificationManager: WeeklyStatsRoundupNotificationManager,
    val analyticsTracker: BloggingRemindersAnalyticsTracker
) {
    fun notify(siteId: Int) {
        val context = contextProvider.getContext()
        val site = siteStore.getSiteByLocalId(siteId) ?: return
        val siteName = SiteUtils.getSiteNameOrHomeURL(site)

        val reminderNotification = WeeklyStatsRoundupNotification(
                channel = resourceProvider.getString(string.notification_channel_reminder_id),
                contentIntentBuilder = {
                    PendingIntent.getActivity(
                            context,
                            NotificationPushIds.REMINDER_NOTIFICATION_ID + siteId,
                            PostsListActivity.buildIntent(
                                    context,
                                    site,
                                    DRAFTS,
                                    actionsShownByDefault = true,
                                    notificationType = BLOGGING_REMINDERS
                            ),
                            PendingIntent.FLAG_CANCEL_CURRENT
                    )
                },
                contentTitle = resourceProvider.getString(string.blogging_reminders_notification_title, siteName),
                contentText = resourceProvider.getString(string.blogging_reminders_notification_text),
                priority = NotificationCompat.PRIORITY_DEFAULT,
                category = NotificationCompat.CATEGORY_REMINDER,
                autoCancel = true,
                colorized = true,
                color = resourceProvider.getColor(color.blue_50),
                smallIcon = drawable.ic_app_white_24dp
        )

        weeklyStatsRoundupNotificationManager.notify(
                NotificationPushIds.REMINDER_NOTIFICATION_ID + siteId,
                reminderNotification)

        analyticsTracker.setSite(siteId)
        analyticsTracker.trackNotificationReceived()
    }

    fun shouldNotify(siteId: Int) =
            siteId != NO_SITE_ID && siteStore.getSiteByLocalId(siteId) != null && accountStore.hasAccessToken()

    companion object {
        const val NO_SITE_ID = -1
    }
}
