package org.wordpress.android.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.Worker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.runBlocking
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.uploads.UploadStarter
import java.util.concurrent.TimeUnit.HOURS

class UploadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val uploadStarter: UploadStarter,
    private val siteStore: SiteStore
) : Worker(appContext, workerParams) {
    companion object {
        private const val UPLOAD_FROM_ALL_SITES = -1
    }

    override fun doWork(): Result {
        runBlocking {
            val job = when (val localSiteId = inputData.getInt(WordPress.LOCAL_SITE_ID, UPLOAD_FROM_ALL_SITES)) {
                UPLOAD_FROM_ALL_SITES -> uploadStarter.queueUploadFromAllSites()
                else -> siteStore.getSiteByLocalId(localSiteId)?.let { uploadStarter.queueUploadFromSite(it) }
            }
            job?.join()
        }
        return Result.success()
    }

    class Factory(
        private val uploadStarter: UploadStarter,
        private val siteStore: SiteStore
    ) : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? {
            return if (workerClassName == UploadWorker::class.java.name) {
                UploadWorker(appContext, workerParameters, uploadStarter, siteStore)
            } else {
                null
            }
        }
    }
}

private fun getUploadConstraints(): Constraints {
    return Constraints.Builder()
        .setRequiredNetworkType(NetworkType.NOT_ROAMING)
        .build()
}

fun enqueueUploadWorkRequestForSite(site: SiteModel): Pair<WorkRequest, Operation> {
    val request = OneTimeWorkRequestBuilder<UploadWorker>()
        .setConstraints(getUploadConstraints())
        .setInputData(workDataOf(WordPress.LOCAL_SITE_ID to site.id))
        .build()
    val operation = WorkManager.getInstance(WordPress.getContext()).enqueueUniqueWork(
        "auto-upload-" + site.id,
        ExistingWorkPolicy.KEEP, request
    )
    return Pair(request, operation)
}

fun enqueuePeriodicUploadWorkRequestForAllSites(): Pair<WorkRequest, Operation> {
    val request = PeriodicWorkRequestBuilder<UploadWorker>(8, HOURS, 6, HOURS)
        .setConstraints(getUploadConstraints())
        .build()
    val operation = WorkManager.getInstance(WordPress.getContext()).enqueueUniquePeriodicWork(
        "periodic auto-upload",
        ExistingPeriodicWorkPolicy.KEEP, request
    )
    return Pair(request, operation)
}
