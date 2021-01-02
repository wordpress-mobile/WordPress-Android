package org.wordpress.android.ui.jetpack.backup.download.usecases

import kotlinx.coroutines.flow.flow
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadRequestTypes
import org.wordpress.android.ui.jetpack.backup.download.handlers.BackupDownloadHandler.BackupDownloadHandlerStatus
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

class PostBackupDownloadUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val activityLogStore: ActivityLogStore
) {
    suspend fun postBackupDownloadRequest(
        rewindId: String,
        site: SiteModel,
        types: BackupDownloadRequestTypes
    ) = flow {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            emit(BackupDownloadHandlerStatus.Failure(rewindId,UiStringRes(R.string.error_network_connection)))
            return@flow
        }

        val result = activityLogStore.backupDownload(BackupDownloadPayload(site, rewindId, types))
        if (result.isError) {
            val message: UiString = if (result.error.message != null) {
                UiStringText(result.error.message.toString())
            } else {
                UiStringRes(R.string.backup_download_generic_failure)
            }
            emit(BackupDownloadHandlerStatus.Failure(rewindId, message))
        } else {
            if (result.rewindId == rewindId) {
                if (result.downloadId == null) {
                    emit(BackupDownloadHandlerStatus.Failure(rewindId, UiStringRes(R.string.backup_download_generic_failure)))
                } else {
                    emit(BackupDownloadHandlerStatus.Success(result.rewindId, result.downloadId as Long))
                }
            } else {
                emit(BackupDownloadHandlerStatus.Failure(rewindId,UiStringRes(R.string.backup_download_another_download_running)))
            }
        }
    }
}
