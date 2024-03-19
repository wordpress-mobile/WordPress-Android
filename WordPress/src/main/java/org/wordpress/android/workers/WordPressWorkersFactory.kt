package org.wordpress.android.workers

import androidx.work.DelegatingWorkerFactory
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.support.ZendeskHelper
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import org.wordpress.android.ui.posts.PostUtilsWrapper
import org.wordpress.android.ui.uploads.UploadPostWorker
import org.wordpress.android.ui.uploads.UploadStarter
import org.wordpress.android.util.UploadWorker
import org.wordpress.android.workers.notification.local.LocalNotificationHandlerFactory
import org.wordpress.android.workers.notification.local.LocalNotificationWorker
import org.wordpress.android.workers.notification.push.GCMRegistrationWorker
import org.wordpress.android.workers.reminder.ReminderNotifier
import org.wordpress.android.workers.reminder.ReminderScheduler
import org.wordpress.android.workers.reminder.ReminderWorker
import org.wordpress.android.workers.reminder.prompt.PromptReminderNotifier
import org.wordpress.android.workers.weeklyroundup.WeeklyRoundupNotifier
import org.wordpress.android.workers.weeklyroundup.WeeklyRoundupWorker
import javax.inject.Inject

class WordPressWorkersFactory @Inject constructor(
    uploadStarter: UploadStarter,
    siteStore: SiteStore,
    localNotificationHandlerFactory: LocalNotificationHandlerFactory,
    reminderScheduler: ReminderScheduler,
    reminderNotifier: ReminderNotifier,
    weeklyRoundupNotifier: WeeklyRoundupNotifier,
    promptReminderNotifier: PromptReminderNotifier,
    accountStore: AccountStore,
    zendeskHelper: ZendeskHelper,
    postStore: PostStore,
    uploadStore: UploadStore,
    mediaStore: MediaStore,
    dispatcher: Dispatcher,
    systemNotificationsTracker: SystemNotificationsTracker,
    postUtilsWrapper: PostUtilsWrapper
) : DelegatingWorkerFactory() {
    init {
        addFactory(UploadWorker.Factory(uploadStarter, siteStore))
        addFactory(LocalNotificationWorker.Factory(localNotificationHandlerFactory))
        addFactory(ReminderWorker.Factory(reminderScheduler, reminderNotifier, promptReminderNotifier))
        addFactory(WeeklyRoundupWorker.Factory(weeklyRoundupNotifier))
        addFactory(GCMRegistrationWorker.Factory(accountStore, zendeskHelper))
        addFactory(UploadPostWorker.Factory(
            siteStore,
            postStore,
            uploadStore,
            mediaStore,
            dispatcher,
            systemNotificationsTracker,
            postUtilsWrapper
        ))
    }
}
