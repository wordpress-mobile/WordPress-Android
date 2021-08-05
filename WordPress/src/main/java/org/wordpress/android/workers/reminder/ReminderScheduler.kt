package org.wordpress.android.workers.reminder

import androidx.work.Data
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.wordpress.android.viewmodel.ContextProvider
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class ReminderScheduler @Inject constructor(
    val contextProvider: ContextProvider
) {
    val workManager by lazy { WorkManager.getInstance(contextProvider.getContext()) }
    var hour: Int = DEFAUlT_START_HOUR
    var minute: Int = DEFAULT_START_MINUTE

    fun schedule(siteId: Int, reminderConfig: ReminderConfig) {
        val uniqueName = getUniqueName(siteId)
        val next = reminderConfig.calculateNext().atTime(LocalTime.of(hour, minute))
        val delay = Duration.between(LocalDateTime.now(), next)
        val inputData = Data.Builder()
                .putInt(REMINDER_SITE_ID, siteId)
                .putAll(reminderConfig.toMap())
                .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .addTag(REMINDER_TAG)
                .setInitialDelay(delay.toMillis(), MILLISECONDS)
                .setInputData(inputData)
                .build()

        workManager.enqueueUniqueWork(uniqueName, REPLACE, workRequest)
    }

    fun cancelById(id: UUID) = workManager.cancelWorkById(id)

    fun cancelBySiteId(siteId: Int) = workManager.cancelUniqueWork(getUniqueName(siteId))

    fun cancelAll() = workManager.cancelAllWorkByTag(REMINDER_TAG)

    fun getById(id: UUID) = workManager.getWorkInfoByIdLiveData(id)

    fun getBySiteId(siteId: Int) = workManager.getWorkInfosForUniqueWorkLiveData(getUniqueName(siteId))

    fun getAll() = workManager.getWorkInfosByTagLiveData(REMINDER_TAG)

    private fun getUniqueName(siteId: Int) = "${REMINDER_TAG}_$siteId"

    companion object {
        private const val REMINDER_TAG = "reminder"
        const val REMINDER_SITE_ID = "reminder_site_id"
        const val DEFAUlT_START_HOUR = 10
        const val DEFAULT_START_MINUTE = 0
    }
}
