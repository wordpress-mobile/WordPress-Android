package org.wordpress.android.workers.reminder

import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.CATEGORY_REMINDER
import androidx.core.app.NotificationCompat.PRIORITY_DEFAULT
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.bloggingprompts.BloggingPrompt
import org.wordpress.android.push.NotificationPushIds.REMINDER_NOTIFICATION_ID
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.notifications.DismissNotificationReceiver
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.config.BloggingPromptsNotificationConfig
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class PromptReminderNotifier @Inject constructor(
    val contextProvider: ContextProvider,
    val resourceProvider: ResourceProvider,
    val siteStore: SiteStore,
    val accountStore: AccountStore,
    val reminderNotificationManager: ReminderNotificationManager,
    val bloggingPromptsNotificationConfig: BloggingPromptsNotificationConfig
) {
    // TODO @RenanLukas replace with remote field in SiteModel after endpoint integration
    var hasOptedInBloggingPromptsReminders = true

    @Suppress("MaxLineLength")
    /* ktlint-disable max-line-length */
    fun notify(siteId: Int) {
        val notificationId = REMINDER_NOTIFICATION_ID + siteId
        val context = contextProvider.getContext()
        val site = siteStore.getSiteByLocalId(siteId) ?: return
        val siteName = SiteUtils.getSiteNameOrHomeURL(site)
        // TODO @RenanLukas get BloggingPrompt from Store when it's ready
        val bloggingPrompt = BloggingPrompt(
            text = "Cast the movie of your life.",
            content = "<!-- wp:pullquote -->\n" +
                    "<figure class=\"wp-block-pullquote\"><blockquote><p>You have 15 minutes to address the whole world live (on television or radio — choose your format). What would you say?</p><cite>(courtesy of plinky.com)</cite></blockquote></figure>\n" +
                    "<!-- /wp:pullquote -->",
            respondents = emptyList()
        )
        val openEditorRequestCode = notificationId + 1
        val openEditorPendingIntent = PendingIntent.getActivity(
            context,
            openEditorRequestCode,
            // TODO @RenanLukas send BloggingPrompt with OpenEditor action when prompt store is ready
            ActivityLauncher.openEditorAndDismissNotificationIntent(context, notificationId),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismissNotificationRequestCode = notificationId + 2
        val dismissIntent = DismissNotificationReceiver.newIntent(context, notificationId)
        val dismissNotificationPendingIntent = PendingIntent.getBroadcast(
            context,
            dismissNotificationRequestCode,
            dismissIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val answerPromptReminderNotification = ReminderNotification(
            channel = resourceProvider.getString(R.string.notification_channel_reminder_id),
            contentIntentBuilder = { openEditorPendingIntent },
            contentTitle = resourceProvider.getString(
                    R.string.blogging_prompts_answer_prompt_notification_title, siteName
            ),
            contentText = bloggingPrompt.text,
            priority = PRIORITY_DEFAULT,
            category = CATEGORY_REMINDER,
            autoCancel = true,
            colorized = true,
            color = resourceProvider.getColor(R.color.blue_50),
            smallIcon = R.drawable.ic_app_white_24dp,
            firstAction = NotificationCompat.Action.Builder(
                0, resourceProvider.getString(R.string.blogging_prompts_answer_prompt_notification_answer_action),
                openEditorPendingIntent
            ).build(),
            secondAction = NotificationCompat.Action.Builder(
                0, resourceProvider.getString(R.string.blogging_prompts_notification_dismiss),
                dismissNotificationPendingIntent
            ).build()
        )

        reminderNotificationManager.notify(notificationId, answerPromptReminderNotification)

        // TODO @RenanLukas track prompt reminder with specific events after definition
        //  analyticsTracker.setSite(siteId)
        //  analyticsTracker.trackNotificationReceived()
    }

    fun shouldNotify(siteId: Int): Boolean {
        val hasAccessToken = accountStore.hasAccessToken()
        val isBloggingPromptsNotificationEnabled = bloggingPromptsNotificationConfig.isEnabled()
        val siteModel = siteStore.getSiteByLocalId(siteId)
        val hasOptedInBloggingPromptsReminders = siteModel != null && hasOptedInBloggingPromptsReminders
        return hasAccessToken && isBloggingPromptsNotificationEnabled && hasOptedInBloggingPromptsReminders
    }

    companion object {
        const val NO_SITE_ID = -1
    }
}
