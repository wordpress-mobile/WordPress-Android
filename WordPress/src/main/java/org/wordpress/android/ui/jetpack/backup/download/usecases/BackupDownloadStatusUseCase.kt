package org.wordpress.android.ui.jetpack.backup.download.usecases

import kotlinx.coroutines.flow.flow
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchBackupDownloadStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.OnBackupDownloadStatusFetched
import org.wordpress.android.ui.jetpack.backup.download.handlers.BackupDownloadStatusHandler.BackupDownloadStatusHandlerState.Complete
import org.wordpress.android.ui.jetpack.backup.download.handlers.BackupDownloadStatusHandler.BackupDownloadStatusHandlerState.Progress
import org.wordpress.android.ui.jetpack.backup.download.handlers.BackupDownloadStatusHandler.BackupDownloadStatusHandlerState.Error
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

class BackupDownloadStatusUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val activityLogStore: ActivityLogStore
) {
    suspend fun getBackupDownloadStatus(site: SiteModel, downloadId: Long) = flow {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            emit(Error(UiStringRes(R.string.error_network_connection)))
            return@flow
        }
        var result: OnBackupDownloadStatusFetched?
        while (true) {
            val downloadStatusForSite = activityLogStore.getBackupDownloadStatusForSite(site)
            if (downloadStatusForSite != null && downloadStatusForSite.downloadId == downloadId) {
                if (downloadStatusForSite.progress == null && downloadId == downloadStatusForSite.downloadId) {
                    emit(Complete(
                            downloadStatusForSite.rewindId,
                            downloadStatusForSite.downloadId,
                            downloadStatusForSite.url))
                    return@flow
                } else {
                    emit(Progress(downloadStatusForSite.rewindId, downloadStatusForSite.progress))
                }
            }
            result = activityLogStore.fetchBackupDownloadState(FetchBackupDownloadStatePayload(site))
            if (result.isError) {
                val message: UiString = if (result.error.message != null) {
                    UiStringText(result.error.message.toString())
                } else {
                    UiStringRes(R.string.backup_download_generic_failure)
                }
                emit(Error(message))
                return@flow
            }
        }
    }
}
