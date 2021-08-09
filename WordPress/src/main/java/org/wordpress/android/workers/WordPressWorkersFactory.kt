package org.wordpress.android.workers

import androidx.work.DelegatingWorkerFactory
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.uploads.UploadStarter
import org.wordpress.android.util.UploadWorker
import org.wordpress.android.workers.reminder.ReminderNotifier
import org.wordpress.android.workers.reminder.ReminderScheduler
import org.wordpress.android.workers.reminder.ReminderWorker
import org.wordpress.android.workers.weeklystatsroundup.WeeklyStatsRoundupNotifier
import org.wordpress.android.workers.weeklystatsroundup.WeeklyStatsRoundupScheduler
import org.wordpress.android.workers.weeklystatsroundup.WeeklyStatsRoundupWorker
import javax.inject.Inject

class WordPressWorkersFactory @Inject constructor(
    uploadStarter: UploadStarter,
    siteStore: SiteStore,
    localNotificationHandlerFactory: LocalNotificationHandlerFactory,
    reminderScheduler: ReminderScheduler,
    reminderNotifier: ReminderNotifier,
    weeklyStatsRoundupScheduler: WeeklyStatsRoundupScheduler,
    weeklyStatsRoundupNotifier: WeeklyStatsRoundupNotifier
) : DelegatingWorkerFactory() {
    init {
        addFactory(UploadWorker.Factory(uploadStarter, siteStore))
        addFactory(LocalNotificationWorker.Factory(localNotificationHandlerFactory))
        addFactory(ReminderWorker.Factory(reminderScheduler, reminderNotifier))
        addFactory(WeeklyStatsRoundupWorker.Factory(weeklyStatsRoundupScheduler, weeklyStatsRoundupNotifier))
    }
}
