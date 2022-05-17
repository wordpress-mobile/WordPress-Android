package org.wordpress.android.workers.reminder.prompt

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat.Action.Builder
import androidx.core.app.NotificationCompat.CATEGORY_REMINDER
import androidx.core.app.NotificationCompat.PRIORITY_DEFAULT
import org.wordpress.android.R.color
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_REMINDERS_NOTIFICATION_PROMPT_ANSWER_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_REMINDERS_NOTIFICATION_PROMPT_TAPPED
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.bloggingprompts.BloggingPrompt
import org.wordpress.android.push.NotificationPushIds.REMINDER_NOTIFICATION_ID
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAnalyticsTracker
import org.wordpress.android.ui.notifications.DismissNotificationReceiver
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.config.BloggingPromptsFeatureConfig
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.workers.reminder.ReminderNotification
import org.wordpress.android.workers.reminder.ReminderNotificationManager
import javax.inject.Inject

class PromptReminderNotifier @Inject constructor(
    val contextProvider: ContextProvider,
    val resourceProvider: ResourceProvider,
    val siteStore: SiteStore,
    val accountStore: AccountStore,
    val reminderNotificationManager: ReminderNotificationManager,
    val bloggingPromptsFeatureConfig: BloggingPromptsFeatureConfig,
    val analyticsTracker: BloggingRemindersAnalyticsTracker
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
            id = 1234,
            text = "Cast the movie of your life.",
            content = "<!-- wp:pullquote -->\n" +
                    "<figure class=\"wp-block-pullquote\"><blockquote><p>You have 15 minutes to address the whole world live (on television or radio — choose your format). What would you say?</p><cite>(courtesy of plinky.com)</cite></blockquote></figure>\n" +
                    "<!-- /wp:pullquote -->",
            respondents = emptyList()
        )
        val openEditorRequestCode = notificationId + 1
        val contentPendingIntent = createContentPendingIntent(
                context, openEditorRequestCode, notificationId, bloggingPrompt
        )
        val openEditorPendingIntent = createOpenEditorPendingIntent(
                context, openEditorRequestCode, notificationId, bloggingPrompt
        )
        val dismissNotificationRequestCode = notificationId + 2
        val dismissButtonIntent = DismissNotificationReceiver.newIntent(
                context, notificationId, Stat.BLOGGING_REMINDERS_NOTIFICATION_PROMPT_DISMISS_TAPPED
        )
        val dismissNotificationButtonPendingIntent = createDismissNotificationButtonPendingIntent(
                context, dismissNotificationRequestCode, dismissButtonIntent
        )
        val dismissSwipeIntent = DismissNotificationReceiver.newIntent(
                context, notificationId, Stat.BLOGGING_REMINDERS_NOTIFICATION_PROMPT_DISMISSED
        )
        val dismissNotificationSwipePendingIntent = createDismissNotificationSwipePendingIntent(
                context, dismissNotificationRequestCode, dismissSwipeIntent
        )
        val answerPromptReminderNotification = ReminderNotification(
            channel = resourceProvider.getString(string.notification_channel_reminder_id),
            contentIntentBuilder = { contentPendingIntent },
            deleteIntentBuilder = { dismissNotificationSwipePendingIntent },
            contentTitle = resourceProvider.getString(
                    string.blogging_prompts_answer_prompt_notification_title, siteName
            ),
            contentText = bloggingPrompt.text,
            priority = PRIORITY_DEFAULT,
            category = CATEGORY_REMINDER,
            autoCancel = true,
            colorized = true,
            color = resourceProvider.getColor(color.blue_50),
            smallIcon = drawable.ic_app_white_24dp,
            firstAction = Builder(
                0, resourceProvider.getString(string.blogging_prompts_answer_prompt_notification_answer_action),
                openEditorPendingIntent
            ).build(),
            secondAction = Builder(
                0, resourceProvider.getString(string.blogging_prompts_notification_dismiss),
                dismissNotificationButtonPendingIntent
            ).build()
        )
        reminderNotificationManager.notify(notificationId, answerPromptReminderNotification)
        analyticsTracker.trackNotificationReceived(true)
    }

    private fun createDismissNotificationButtonPendingIntent(
        context: Context,
        dismissNotificationRequestCode: Int,
        dismissButtonIntent: Intent
    ) = PendingIntent.getBroadcast(
            context,
            dismissNotificationRequestCode,
            dismissButtonIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun createDismissNotificationSwipePendingIntent(
        context: Context,
        dismissNotificationRequestCode: Int,
        dismissSwipeIntent: Intent
    ) = PendingIntent.getBroadcast(
            context,
            dismissNotificationRequestCode,
            dismissSwipeIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun createOpenEditorPendingIntent(
        context: Context,
        openEditorRequestCode: Int,
        notificationId: Int,
        bloggingPrompt: BloggingPrompt
    ) = PendingIntent.getActivity(
            context,
            openEditorRequestCode,
            ActivityLauncher.openEditorWithPromptAndDismissNotificationIntent(
                    context,
                    notificationId,
                    bloggingPrompt,
                    BLOGGING_REMINDERS_NOTIFICATION_PROMPT_ANSWER_TAPPED
            ),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun createContentPendingIntent(
        context: Context,
        openEditorRequestCode: Int,
        notificationId: Int,
        bloggingPrompt: BloggingPrompt
    ) = PendingIntent.getActivity(
            context,
            openEditorRequestCode,
            ActivityLauncher.openEditorWithPromptAndDismissNotificationIntent(
                    context,
                    notificationId,
                    bloggingPrompt,
                    BLOGGING_REMINDERS_NOTIFICATION_PROMPT_TAPPED
            ),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun shouldNotify(siteId: Int): Boolean {
        val hasAccessToken = accountStore.hasAccessToken()
        val isBloggingPromptsEnabled = bloggingPromptsFeatureConfig.isEnabled()
        val siteModel = siteStore.getSiteByLocalId(siteId)
        val hasOptedInBloggingPromptsReminders = siteModel != null && hasOptedInBloggingPromptsReminders
        return hasAccessToken && isBloggingPromptsEnabled && hasOptedInBloggingPromptsReminders
    }

    companion object {
        const val NO_SITE_ID = -1
    }
}
