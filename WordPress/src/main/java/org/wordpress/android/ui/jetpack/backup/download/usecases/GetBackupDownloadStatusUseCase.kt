package org.wordpress.android.ui.jetpack.backup.download.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.BackupDownloadStatusModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchBackupDownloadStatePayload
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Complete
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Empty
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.NetworkUnavailable
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Progress
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import javax.inject.Named

const val DELAY_MILLIS = 5000L
const val MAX_RETRY = 3

class GetBackupDownloadStatusUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val activityLogStore: ActivityLogStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    suspend fun getBackupDownloadStatus(
        site: SiteModel,
        downloadId: Long? = null
    ) = flow {
        var retryAttempts = 0
        if (downloadId == null) {
            retryAttempts = -1
            if (!isNetworkAvailable()) return@flow
            val result = activityLogStore.fetchBackupDownloadState(FetchBackupDownloadStatePayload(site))
            if (result.isError) {
                if (retryAttempts++ >= MAX_RETRY) {
                    emit(RemoteRequestFailure)
                    return@flow
                }
            }
        }
        while (true) {
            if (!isNetworkAvailable()) return@flow

            val result = activityLogStore.fetchBackupDownloadState(FetchBackupDownloadStatePayload(site))
            if (result.isError) {
                if (retryAttempts++ >= MAX_RETRY) {
                    emit(RemoteRequestFailure)
                    return@flow
                }
            } else {
                val status = activityLogStore.getBackupDownloadStatusForSite(site)
                if (status == null) {
                    emit(Empty)
                    return@flow
                }
                if (downloadId == null || status.downloadId == downloadId) {
                    if (emitCompleteElseProgress(status)) return@flow
                }
                delay(DELAY_MILLIS)
            }
        }
    }.flowOn(bgDispatcher)

    private suspend fun FlowCollector<BackupDownloadRequestState>.isNetworkAvailable(): Boolean {
        return if (!networkUtilsWrapper.isNetworkAvailable()) {
            emit(NetworkUnavailable)
            false
        } else true
    }

    private suspend fun FlowCollector<BackupDownloadRequestState>.emitCompleteElseProgress(
        status: BackupDownloadStatusModel
    ): Boolean {
        val published = activityLogStore.getActivityLogItemByRewindId(status.rewindId)?.published
        return if (status.progress == null) {
            emit(Complete(status.rewindId, status.downloadId, status.url, published))
            true
        } else {
            emit(Progress(status.rewindId, status.progress, published))
            false
        }
    }
}
