package org.wordpress.android.workers.reminder

import androidx.work.Data
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.wordpress.android.viewmodel.ContextProvider
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class ReminderScheduler @Inject constructor(
    val contextProvider: ContextProvider
) {
    val workManager by lazy { WorkManager.getInstance(contextProvider.getContext()) }

    fun schedule(siteId: Long, reminderConfig: ReminderConfig): LocalDateTime {
        val uniqueName = getUniqueName(siteId)
        val next = reminderConfig.calculateNext().atTime(8, 0)
        val delay = Duration.between(LocalDateTime.now(), next)
        val inputData = Data.Builder()
                .putLong(REMINDER_SITE_ID, siteId)
                .putAll(reminderConfig.toMap())
                .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .addTag(REMINDER_TAG)
                .setInitialDelay(delay.toMillis(), MILLISECONDS)
                .setInputData(inputData)
                .build()

        workManager.enqueueUniqueWork(uniqueName, REPLACE, workRequest)

        return next
    }

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
