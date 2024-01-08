package org.wordpress.android.push

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.workers.notification.push.GCMRegistrationWorker
import javax.inject.Inject

class GCMRegistrationScheduler @Inject constructor(
    val contextProvider: ContextProvider
) {
    private val workManager by lazy { WorkManager.getInstance(contextProvider.getContext()) }

    fun scheduleRegistration() {
        workManager.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            buildOneTimeWorkRequest()
        )
    }

    private fun buildOneTimeWorkRequest() =
        OneTimeWorkRequestBuilder<GCMRegistrationWorker>()
            .addTag(UNIQUE_WORK_NAME)
            .build()

    companion object {
        const val UNIQUE_WORK_NAME = "GCMRegistrationWork"
    }
}
