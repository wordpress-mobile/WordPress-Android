package org.wordpress.android.ui.jetpack.backup.download.usecases

import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchBackupDownloadStatePayload
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Complete
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.NetworkUnavailable
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Progress
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import kotlinx.coroutines.delay
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Empty
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

const val DELAY_MILLIS = 5000L
const val MAX_RETRY = 3

class GetBackupDownloadStatusUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val activityLogStore: ActivityLogStore
) {
    val tag = javaClass.simpleName
    suspend fun getBackupDownloadStatus(site: SiteModel, downloadId: Long) = flow {
        var retryAttempts = 0
        while (true) {
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                emit(NetworkUnavailable)
                return@flow
            }

            val result = activityLogStore.fetchBackupDownloadState(FetchBackupDownloadStatePayload(site))
            if (result.isError) {
                if (retryAttempts++ >= MAX_RETRY) {
                    AppLog.d(T.JETPACK_BACKUP, "$tag Exceeded 3 retries while fetching status")
                    emit(RemoteRequestFailure)
                    return@flow
                }
            } else {
                val status = activityLogStore.getBackupDownloadStatusForSite(site)
                if (status == null) {
                    emit(Empty)
                    return@flow
                }
                if (status.downloadId == downloadId) {
                    if (status.progress == null && downloadId == status.downloadId) {
                        emit(Complete(status.rewindId, status.downloadId, status.url))
                        return@flow
                    } else {
                        emit(Progress(status.rewindId, status.progress))
                    }
                }
                delay(DELAY_MILLIS)
            }
        }
    }
}
