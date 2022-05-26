package org.wordpress.android.workers.reminder

import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.CATEGORY_REMINDER
import androidx.core.app.NotificationCompat.PRIORITY_DEFAULT
import kotlinx.coroutines.flow.firstOrNull
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore
import org.wordpress.android.push.NotificationPushIds.REMINDER_NOTIFICATION_ID
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.notifications.DismissNotificationReceiver
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.config.BloggingPromptsFeatureConfig
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider
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
    private val bloggingRemindersStore: BloggingRemindersStore
) {
    suspend fun notify(siteId: Int) {
        val notificationId = REMINDER_NOTIFICATION_ID + siteId
        val context = contextProvider.getContext()
        val site = siteStore.getSiteByLocalId(siteId) ?: return
        val siteName = SiteUtils.getSiteNameOrHomeURL(site)

        val prompt = bloggingPromptsStore.getPromptForDate(site, Date()).firstOrNull()?.model

        val openEditorRequestCode = notificationId + 1
        val openEditorPendingIntent = PendingIntent.getActivity(
                context,
                openEditorRequestCode,
                ActivityLauncher.openEditorWithPromptAndDismissNotificationIntent(
                        context,
                        notificationId,
                        prompt
                ),
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
                contentText = prompt?.text ?: "",
                priority = PRIORITY_DEFAULT,
                category = CATEGORY_REMINDER,
                autoCancel = true,
                colorized = true,
                color = resourceProvider.getColor(R.color.blue_50),
                smallIcon = R.drawable.ic_app_white_24dp,
                firstAction = NotificationCompat.Action.Builder(
                        0,
                        resourceProvider.getString(R.string.blogging_prompts_answer_prompt_notification_answer_action),
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

    suspend fun shouldNotify(siteId: Int): Boolean {
        val hasAccessToken = accountStore.hasAccessToken()
        val isBloggingPromptsEnabled = bloggingPromptsFeatureConfig.isEnabled()
        val siteModel = siteStore.getSiteByLocalId(siteId)

        val isPromptOptedIn = bloggingRemindersStore.bloggingRemindersModel(siteId).firstOrNull()?.isPromptIncluded
                ?: false
        // TODO @klymyam check the value from the site model when we will start syncing the settings with remote
        // val isPromptOptedIn = siteModel.isBloggingPromptsOptedIn

        val hasOptedInBloggingPromptsReminders = siteModel != null && isPromptOptedIn
        return hasAccessToken && isBloggingPromptsEnabled && hasOptedInBloggingPromptsReminders
    }

    companion object {
        const val NO_SITE_ID = -1
    }
}
