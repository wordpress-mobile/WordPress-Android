package org.wordpress.android.workers.weeklyroundup

import android.content.Context
import android.content.SharedPreferences
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy.KEEP
import androidx.work.NetworkType.CONNECTED
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.wordpress.android.R
import java.time.DayOfWeek.MONDAY
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters.next
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class WeeklyRoundupScheduler @Inject constructor(
    private val context: Context,
    private val sharedPrefs: SharedPreferences
) {
    private val workManager by lazy { WorkManager.getInstance(context) }

    fun scheduleIfNeeded() {
        val isNotificationSettingsEnabled = sharedPrefs.getBoolean(
            context.getString(R.string.wp_pref_notifications_main),
            true
        )
        if (isNotificationSettingsEnabled) {
            val next = LocalDate.now().with(next(MONDAY)).atTime(DEFAULT_START_TIME)
            val delay = Duration.between(LocalDateTime.now(), next)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<WeeklyRoundupWorker>()
                .addTag(TAG)
                .setInitialDelay(delay.toMillis(), MILLISECONDS)
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniqueWork(TAG, KEEP, workRequest)
        }
    }

    fun cancel() = workManager.cancelUniqueWork(TAG)

    fun getAll() = workManager.getWorkInfosForUniqueWorkLiveData(TAG)

    companion object {
        private const val TAG = "weekly_roundup"
        private val DEFAULT_START_TIME = LocalTime.of(10, 0)
    }
}
