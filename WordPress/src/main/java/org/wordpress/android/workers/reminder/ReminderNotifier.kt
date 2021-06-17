package org.wordpress.android.workers.reminder

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.CATEGORY_REMINDER
import androidx.core.app.NotificationCompat.PRIORITY_DEFAULT
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.push.NotificationPushIds.REMINDER_NOTIFICATION_ID
import org.wordpress.android.ui.posts.PostsListActivity
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class ReminderNotifier @Inject constructor(
    val contextProvider: ContextProvider,
    val siteStore: SiteStore,
    val accountStore: AccountStore
) {
    fun notify(siteId: Long) {
        val context = contextProvider.getContext()
        val site = siteStore.getSiteBySiteId(siteId)
        val name = accountStore.account.firstName

        val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                PostsListActivity.buildIntent(context, site),
                FLAG_CANCEL_CURRENT
        )
        val builder = NotificationCompat.Builder(context, context.getString(R.string.notification_channel_reminder_id))
                .setContentIntent(pendingIntent)
                .setContentTitle(context.getString(R.string.blogging_reminders_notification_title, name))
                .setContentText(context.getString(R.string.blogging_reminders_notification_text))
                .setPriority(PRIORITY_DEFAULT)
                .setCategory(CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setColorized(true)
                .setColor(ContextCompat.getColor(context, R.color.blue_50))
                .setSmallIcon(R.drawable.ic_app_white_24dp)

        NotificationManagerCompat.from(context).notify(REMINDER_NOTIFICATION_ID, builder.build())
    }

    fun shouldNotify(siteId: Long) = siteId != NO_SITE_ID
            && siteStore.getSiteBySiteId(siteId) != null
            && accountStore.hasAccessToken()

    companion object {
        const val NO_SITE_ID = -1L
    }
}
