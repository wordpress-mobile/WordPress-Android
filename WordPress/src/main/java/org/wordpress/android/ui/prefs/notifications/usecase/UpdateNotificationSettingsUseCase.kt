package org.wordpress.android.ui.prefs.notifications.usecase

import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.workers.notification.createsite.CreateSiteNotificationScheduler
import org.wordpress.android.workers.reminder.ReminderConfig.WeeklyReminder
import org.wordpress.android.workers.reminder.ReminderScheduler
import org.wordpress.android.workers.weeklyroundup.WeeklyRoundupScheduler
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Named

@Suppress("LongParameterList")
class UpdateNotificationSettingsUseCase @Inject constructor(
    private val sharedPrefs: SharedPreferences,
    private val resourceProvider: ResourceProvider,
    private val weeklyRoundupScheduler: WeeklyRoundupScheduler,
    private val reminderScheduler: ReminderScheduler,
    private val createSiteNotificationScheduler: CreateSiteNotificationScheduler,
    private val bloggingRemindersStore: BloggingRemindersStore,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun updateNotificationSettings(enabled: Boolean) {
        updatePref(enabled)

        if (enabled) {
            // The switch is turned on. Schedule local notifications.
            weeklyRoundupScheduler.schedule()
            scheduleSavedBloggingReminders()
            createSiteNotificationScheduler.scheduleCreateSiteNotificationIfNeeded()
        } else {
            // The switch is turned off. Cancel scheduled local notifications.
            weeklyRoundupScheduler.cancel()
            reminderScheduler.cancelAll()
            createSiteNotificationScheduler.cancelCreateSiteNotification()
        }
    }

    private fun updatePref(enabled: Boolean) {
        val editor = sharedPrefs.edit()
        editor.putBoolean(resourceProvider.getString(R.string.wp_pref_notifications_main), enabled)
        editor.apply()
    }

    /**
     * Fetches saved blogging reminders from the db and schedules reminders for them.
     */
    private suspend fun scheduleSavedBloggingReminders() = withContext(ioDispatcher) {
        val bloggingRemindersModelList = bloggingRemindersStore.getAll().first()
        bloggingRemindersModelList.forEach { bloggingRemindersModel ->
            val daysCount = bloggingRemindersModel.enabledDays.size
            if (daysCount > 0) {
                val enabledDaysOfWeek = bloggingRemindersModel.enabledDays.map { DayOfWeek.valueOf(it.name) }
                reminderScheduler.schedule(
                        bloggingRemindersModel.siteId,
                        bloggingRemindersModel.hour,
                        bloggingRemindersModel.minute,
                        WeeklyReminder(enabledDaysOfWeek.toSet())
                )
            }
        }
    }
}
