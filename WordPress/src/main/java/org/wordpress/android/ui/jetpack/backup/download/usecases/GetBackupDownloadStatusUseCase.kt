package org.wordpress.android.ui.jetpack.backup.download.usecases

import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchBackupDownloadStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.OnBackupDownloadStatusFetched
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Complete
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.NetworkUnavailable
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Progress
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import kotlinx.coroutines.delay

const val DELAY_MILLIS = 5000L

class GetBackupDownloadStatusUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val activityLogStore: ActivityLogStore
) {
    suspend fun getBackupDownloadStatus(site: SiteModel, downloadId: Long) = flow {
        var result: OnBackupDownloadStatusFetched?
        while (true) {
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                emit(NetworkUnavailable)
                return@flow
            }

            val downloadStatusForSite = activityLogStore.getBackupDownloadStatusForSite(site)
            if (downloadStatusForSite != null && downloadStatusForSite.downloadId == downloadId) {
                if (downloadStatusForSite.progress == null && downloadId == downloadStatusForSite.downloadId) {
                    emit(
                            Complete(
                                    downloadStatusForSite.rewindId,
                                    downloadStatusForSite.downloadId,
                                    downloadStatusForSite.url
                            )
                    )
                    return@flow
                } else {
                    emit(Progress(downloadStatusForSite.rewindId, downloadStatusForSite.progress))
                }
            }
            result = activityLogStore.fetchBackupDownloadState(FetchBackupDownloadStatePayload(site))
            if (result.isError) {
                emit(RemoteRequestFailure)
                return@flow
            }
            delay(DELAY_MILLIS)
        }
    }
}
