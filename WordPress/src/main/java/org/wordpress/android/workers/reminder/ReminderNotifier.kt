package org.wordpress.android.workers.reminder

import android.app.PendingIntent
import androidx.core.app.NotificationCompat.CATEGORY_REMINDER
import androidx.core.app.NotificationCompat.PRIORITY_DEFAULT
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.push.NotificationPushIds.REMINDER_NOTIFICATION_ID
import org.wordpress.android.push.NotificationType.BLOGGING_REMINDERS
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAnalyticsTracker
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.posts.PostListType
import org.wordpress.android.ui.posts.PostsListActivity
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class ReminderNotifier @Inject constructor(
    val contextProvider: ContextProvider,
    val resourceProvider: ResourceProvider,
    val siteStore: SiteStore,
    val accountStore: AccountStore,
    val reminderNotificationManager: ReminderNotificationManager,
    val analyticsTracker: BloggingRemindersAnalyticsTracker,
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper
) {
    fun notify(siteId: Int) {
        val context = contextProvider.getContext()
        val site = siteStore.getSiteByLocalId(siteId) ?: return
        val siteName = SiteUtils.getSiteNameOrHomeURL(site)

        val reminderNotification = ReminderNotification(
            channel = resourceProvider.getString(R.string.notification_channel_reminder_id),
            contentIntentBuilder = {
                PendingIntent.getActivity(
                    context,
                    REMINDER_NOTIFICATION_ID + siteId,
                    PostsListActivity.buildIntent(
                        context,
                        site,
                        PostListType.DRAFTS,
                        actionsShownByDefault = true,
                        notificationType = BLOGGING_REMINDERS
                    ),
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            },
            contentTitle = resourceProvider.getString(R.string.blogging_reminders_notification_title, siteName),
            contentText = resourceProvider.getString(R.string.blogging_reminders_notification_text),
            priority = PRIORITY_DEFAULT,
            category = CATEGORY_REMINDER,
            autoCancel = true,
            colorized = true,
            color = if (BuildConfig.IS_JETPACK_APP) {
                resourceProvider.getColor(R.color.jetpack_green)
            } else {
                resourceProvider.getColor(R.color.blue_50)
            },
            smallIcon = R.drawable.ic_app_white_24dp
        )

        reminderNotificationManager.notify(REMINDER_NOTIFICATION_ID + siteId, reminderNotification)

        analyticsTracker.setSite(siteId)
        analyticsTracker.trackNotificationReceived(false)
    }

    fun shouldNotify(siteId: Int) =
        siteId != NO_SITE_ID && siteStore.getSiteByLocalId(siteId) != null && accountStore.hasAccessToken() &&
                !jetpackFeatureRemovalPhaseHelper.shouldRemoveJetpackFeatures()

    companion object {
        const val NO_SITE_ID = -1
    }
}
