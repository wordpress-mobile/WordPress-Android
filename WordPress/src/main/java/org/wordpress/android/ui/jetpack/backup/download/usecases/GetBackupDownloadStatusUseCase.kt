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
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import javax.inject.Named

const val DELAY_MILLIS = 1000L
const val MAX_RETRY = 3
const val DELAY_FACTOR = 2

class GetBackupDownloadStatusUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val activityLogStore: ActivityLogStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private val tag = javaClass.simpleName
    suspend fun getBackupDownloadStatus(
        site: SiteModel,
        downloadId: Long? = null
    ) = flow {
        var retryAttempts = 0
        var backoffDelay = DELAY_MILLIS
        while (true) {
            if (!isNetworkAvailable()) return@flow

            val result = activityLogStore.fetchBackupDownloadState(FetchBackupDownloadStatePayload(site))
            if (result.isError) {
                when (exceedsRetryAttempts(retryAttempts++)) {
                    true -> return@flow
                    false -> {
                        delay(backoffDelay)
                        backoffDelay = (backoffDelay * DELAY_FACTOR)
                    }
                }
            } else {
                retryAttempts = 0
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

    private suspend fun FlowCollector<BackupDownloadRequestState>.exceedsRetryAttempts(retryAttempts: Int): Boolean {
        return if (retryAttempts >= MAX_RETRY) {
            AppLog.d(T.JETPACK_BACKUP, "$tag: Exceeded $MAX_RETRY retries while fetching status")
            emit(RemoteRequestFailure)
            true
        } else false
    }
}
