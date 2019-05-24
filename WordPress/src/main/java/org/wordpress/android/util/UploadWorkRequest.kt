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
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.uploads.LocalDraftUploadStarter
import java.util.concurrent.TimeUnit.HOURS

class AutoUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val localDraftUploadStarter: LocalDraftUploadStarter,
    private val siteStore: SiteStore
) : Worker(appContext, workerParams) {
    companion object {
        private const val UPLOAD_FROM_ALL_SITES = -1
    }

    override fun doWork(): Result {
        when (val localSiteId = inputData.getInt(WordPress.LOCAL_SITE_ID, UPLOAD_FROM_ALL_SITES)) {
            UPLOAD_FROM_ALL_SITES -> localDraftUploadStarter.queueUploadFromAllSites()
            else -> siteStore.getSiteByLocalId(localSiteId)?.let { localDraftUploadStarter.queueUploadFromSite(it) }
        }
        return Result.success()
    }

    class Factory(
        private val localDraftUploadStarter: LocalDraftUploadStarter,
        private val siteStore: SiteStore
    ) : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? {
            // TODO This should use the [workerClassName] if there are other of Worker subclasses in the project
            return AutoUploadWorker(appContext, workerParameters, localDraftUploadStarter, siteStore)
        }
    }
}

private fun getUploadConstraints(): Constraints {
    return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_ROAMING)
            .build()
}

fun enqueueUploadWorkRequestForSite(site: SiteModel): Pair<WorkRequest, Operation> {
    val request = OneTimeWorkRequestBuilder<AutoUploadWorker>()
            .setConstraints(getUploadConstraints())
            .setInputData(workDataOf(WordPress.LOCAL_SITE_ID to site.id))
            .build()
    val operation = WorkManager.getInstance().enqueueUniqueWork(
            "auto-upload-" + site.id,
            ExistingWorkPolicy.KEEP, request
    )
    return Pair(request, operation)
}

fun enqueuePeriodicUploadWorkRequestForAllSites(): Pair<WorkRequest, Operation> {
    val request = PeriodicWorkRequestBuilder<AutoUploadWorker>(8, HOURS, 6, HOURS)
            .setConstraints(getUploadConstraints())
            .build()
    val operation = WorkManager.getInstance().enqueueUniquePeriodicWork(
            "periodic auto-upload",
            ExistingPeriodicWorkPolicy.KEEP, request
    )
    return Pair(request, operation)
}
