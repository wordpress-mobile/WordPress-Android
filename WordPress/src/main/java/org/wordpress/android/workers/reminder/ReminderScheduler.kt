package org.wordpress.android.workers.reminder

import androidx.work.Data
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.workers.reminder.ReminderConfig.DailyReminder
import org.wordpress.android.workers.reminder.ReminderConfig.WeeklyReminder
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters.next
import java.util.UUID
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class ReminderScheduler @Inject constructor(
    val contextProvider: ContextProvider
) {
    val workManager by lazy { WorkManager.getInstance(contextProvider.getContext()) }

    fun schedule(siteId: Long, reminderConfig: ReminderConfig): LocalDateTime {
        val uniqueName = getUniqueName(siteId)
        val dateTime = calculateDate(reminderConfig).atTime(8, 0)
        val initialDelay = Duration.between(LocalDateTime.now(), dateTime)
        val inputData = Data.Builder()
                .putLong(REMINDER_SITE_ID, siteId)
                .putAll(reminderConfig.toMap())
                .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .addTag(REMINDER_TAG)
                .setInitialDelay(initialDelay.toMillis(), MILLISECONDS)
                .setInputData(inputData)
                .build()

        workManager.enqueueUniqueWork(uniqueName, REPLACE, workRequest)

        return dateTime
    }

    // TODO Use site timezone instead of local time
    private fun calculateDate(reminderConfig: ReminderConfig) = LocalDate.now().let { today ->
        when (reminderConfig) {
            is DailyReminder -> today.plusDays(1)
            is WeeklyReminder -> today.withNextDayOfWeekFrom(reminderConfig.days)
        }
    }

    private fun LocalDate.withNextDayOfWeekFrom(days: Set<DayOfWeek>) = days.map { with(next(it)) }.minOrNull()!!

    fun cancelById(id: UUID) = workManager.cancelWorkById(id)

    fun cancelBySiteId(siteId: Long) = workManager.cancelUniqueWork(getUniqueName(siteId))

    fun cancelAll() = workManager.cancelAllWorkByTag(REMINDER_TAG)

    fun getById(id: UUID) = workManager.getWorkInfoByIdLiveData(id)

    fun getBySiteId(siteId: Long) = workManager.getWorkInfosForUniqueWorkLiveData(getUniqueName(siteId))

    fun getAll() = workManager.getWorkInfosByTagLiveData(REMINDER_TAG)

    private fun getUniqueName(siteId: Long) = "${REMINDER_TAG}_$siteId"

    companion object {
        private const val REMINDER_TAG = "reminder"
        const val REMINDER_SITE_ID = "reminder_site_id"
    }
}
