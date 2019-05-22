package org.wordpress.android.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.uploads.LocalDraftUploadStarter
import java.util.concurrent.TimeUnit.HOURS
import javax.inject.Inject

class AutoUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    @Inject lateinit var localDraftUploadStarter: LocalDraftUploadStarter
    @Inject lateinit var siteStore: SiteStore

    init {
        (appContext as WordPress).component().inject(this)
    }

    override fun doWork(): Result {
        when (val localSiteId = inputData.getInt(WordPress.LOCAL_SITE_ID, -1)) {
            -1 -> localDraftUploadStarter.queueUploadFromAllSites()
            else -> siteStore.getSiteByLocalId(localSiteId)?.let { localDraftUploadStarter.queueUploadFromSite(it) }
        }
        return Result.success()
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
    val request = PeriodicWorkRequestBuilder<AutoUploadWorker>(6, HOURS)
            .setConstraints(getUploadConstraints())
            .build()
    val operation = WorkManager.getInstance().enqueueUniquePeriodicWork(
            "periodic auto-upload",
            ExistingPeriodicWorkPolicy.KEEP, request
    )
    return Pair(request, operation)
}
