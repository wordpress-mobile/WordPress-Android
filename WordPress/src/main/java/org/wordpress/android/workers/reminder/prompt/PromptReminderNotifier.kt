package org.wordpress.android.workers.reminder.prompt

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat.Action.Builder
import androidx.core.app.NotificationCompat.CATEGORY_REMINDER
import androidx.core.app.NotificationCompat.PRIORITY_DEFAULT
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_REMINDERS_NOTIFICATION_PROMPT_ANSWER_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_REMINDERS_NOTIFICATION_PROMPT_TAPPED
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore
import org.wordpress.android.push.NotificationPushIds.REMINDER_NOTIFICATION_ID
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAnalyticsTracker
import org.wordpress.android.ui.notifications.DismissNotificationReceiver
import org.wordpress.android.ui.posts.PostUtils.EntryPoint
import org.wordpress.android.util.HtmlCompatWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.config.BloggingPromptsFeatureConfig
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.workers.reminder.ReminderNotification
import org.wordpress.android.workers.reminder.ReminderNotificationManager
import java.util.Date
import javax.inject.Inject

class PromptReminderNotifier @Inject constructor(
    val contextProvider: ContextProvider,
    val resourceProvider: ResourceProvider,
    val siteStore: SiteStore,
    val accountStore: AccountStore,
    val reminderNotificationManager: ReminderNotificationManager,
    val bloggingPromptsFeatureConfig: BloggingPromptsFeatureConfig,
    val bloggingPromptsStore: BloggingPromptsStore,
    val bloggingRemindersAnalyticsTracker: BloggingRemindersAnalyticsTracker,
    val htmlCompatWrapper: HtmlCompatWrapper,
    private val bloggingRemindersStore: BloggingRemindersStore
) {
    @Suppress("MagicNumber")
    suspend fun notify(siteId: Int) {
        val notificationId = REMINDER_NOTIFICATION_ID + siteId
        val context = contextProvider.getContext()
        val site = siteStore.getSiteByLocalId(siteId) ?: return

        val prompt = bloggingPromptsStore.getPromptForDate(site, Date()).firstOrNull()?.model
        val contentPendingIntent =
            createContentPendingIntent(context, notificationId + 1, notificationId, prompt)
        val openEditorPendingIntent =
            createOpenEditorPendingIntent(context, notificationId + 2, notificationId, prompt)
        val dismissNotificationButtonPendingIntent = createDismissNotificationButtonPendingIntent(
            context,
            notificationId + 3,
            DismissNotificationReceiver.newIntent(
                context, notificationId, Stat.BLOGGING_REMINDERS_NOTIFICATION_PROMPT_DISMISS_TAPPED
            )
        )
        val dismissNotificationSwipePendingIntent = createDismissNotificationSwipePendingIntent(
            context,
            notificationId + 4,
            DismissNotificationReceiver.newIntent(
                context, notificationId, Stat.BLOGGING_REMINDERS_NOTIFICATION_PROMPT_DISMISSED
            )
        )
        val answerPromptReminderNotification = ReminderNotification(
            channel = resourceProvider.getString(string.notification_channel_reminder_id),
            contentIntentBuilder = { contentPendingIntent },
            deleteIntentBuilder = { dismissNotificationSwipePendingIntent },
            contentTitle = resourceProvider.getString(
                string.blogging_prompts_answer_prompt_notification_title, SiteUtils.getSiteNameOrHomeURL(site)
            ),
            contentText = htmlCompatWrapper.fromHtml(prompt?.text.orEmpty()).toString(),
            priority = PRIORITY_DEFAULT,
            category = CATEGORY_REMINDER,
            autoCancel = true,
            colorized = true,
            color = if (BuildConfig.IS_JETPACK_APP) {
                resourceProvider.getColor(R.color.jetpack_green)
            } else {
                resourceProvider.getColor(R.color.blue_50)
            },
            smallIcon = drawable.ic_app_white_24dp,
            firstAction = Builder(
                0,
                resourceProvider.getString(string.blogging_prompts_answer_prompt_notification_answer_action),
                openEditorPendingIntent
            ).build(),
            secondAction = Builder(
                0,
                resourceProvider.getString(string.blogging_prompts_notification_dismiss),
                dismissNotificationButtonPendingIntent
            ).build()
        )
        reminderNotificationManager.notify(notificationId, answerPromptReminderNotification)
        bloggingRemindersAnalyticsTracker.trackNotificationReceived(true)
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
        bloggingPrompt: BloggingPromptModel?
    ) = PendingIntent.getActivity(
        context,
        openEditorRequestCode,
        ActivityLauncher.openEditorWithPromptAndDismissNotificationIntent(
            context,
            notificationId,
            bloggingPrompt,
            BLOGGING_REMINDERS_NOTIFICATION_PROMPT_ANSWER_TAPPED,
            EntryPoint.BLOGGING_REMINDERS_NOTIFICATION_ANSWER_PROMPT
        ),
        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun createContentPendingIntent(
        context: Context,
        openEditorRequestCode: Int,
        notificationId: Int,
        bloggingPrompt: BloggingPromptModel?
    ) = PendingIntent.getActivity(
        context,
        openEditorRequestCode,
        ActivityLauncher.openEditorWithPromptAndDismissNotificationIntent(
            context,
            notificationId,
            bloggingPrompt,
            BLOGGING_REMINDERS_NOTIFICATION_PROMPT_TAPPED,
            EntryPoint.BLOGGING_REMINDERS_NOTIFICATION_ANSWER_PROMPT
        ),
        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    suspend fun shouldNotify(siteId: Int): Boolean {
        val hasAccessToken = accountStore.hasAccessToken()
        val isBloggingPromptsEnabled = bloggingPromptsFeatureConfig.isEnabled()
        val siteModel = siteStore.getSiteByLocalId(siteId)
        val bloggingRemindersModel = bloggingRemindersStore.bloggingRemindersModel(siteId).first()
        val hasOptedInBloggingPromptsReminders = siteModel != null && bloggingRemindersModel.isPromptIncluded
        return hasAccessToken && isBloggingPromptsEnabled && hasOptedInBloggingPromptsReminders
    }

    companion object {
        const val NO_SITE_ID = -1
    }
}
