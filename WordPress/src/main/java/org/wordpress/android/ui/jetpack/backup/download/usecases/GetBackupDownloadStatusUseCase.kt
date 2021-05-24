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
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.NetworkUnavailable
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Progress
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.max

const val DELAY_MILLIS = 1000L
const val MAX_RETRY = 3
const val DELAY_FACTOR = 2

class GetBackupDownloadStatusUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val activityLogStore: ActivityLogStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private val tag = javaClass.simpleName

    @Suppress("ComplexMethod", "LoopWithTooManyJumpStatements")
    suspend fun getBackupDownloadStatus(
        site: SiteModel,
        downloadId: Long? = null
    ) = flow {
        var retryAttempts = 0
        while (true) {
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                val retryAttemptsExceeded = handleError(retryAttempts++, NetworkUnavailable)
                if (retryAttemptsExceeded) break else continue
            }
            val result = activityLogStore.fetchBackupDownloadState(FetchBackupDownloadStatePayload(site))
            if (result.isError) {
                val retryAttemptsExceeded = handleError(retryAttempts++, RemoteRequestFailure)
                if (retryAttemptsExceeded) break else continue
            }
            retryAttempts = 0
            val status = activityLogStore.getBackupDownloadStatusForSite(site)
            if (status == null) {
                emit(Empty)
                break
            }
            if (downloadId == null || status.downloadId == downloadId) {
                if (emitCompleteElseProgress(status)) break
            }
            delay(DELAY_MILLIS)
        }
    }.flowOn(bgDispatcher)

    private suspend fun FlowCollector<BackupDownloadRequestState>.emitCompleteElseProgress(
        status: BackupDownloadStatusModel
    ): Boolean {
        val published = activityLogStore.getActivityLogItemByRewindId(status.rewindId)?.published
        return if (status.progress == null) {
            val isValid = isValid(status.url, status.validUntil, status.downloadId)
            emit(Complete(status.rewindId, status.downloadId, status.url, published, status.validUntil, isValid))
            true
        } else {
            emit(Progress(status.rewindId, status.progress, published))
            false
        }
    }

    private suspend fun FlowCollector<BackupDownloadRequestState>.handleError(
        retryAttempts: Int,
        failure: Failure
    ): Boolean {
        return if (retryAttempts >= MAX_RETRY) {
            AppLog.d(T.JETPACK_BACKUP, "$tag: Exceeded $MAX_RETRY retries while fetching status")
            emit(failure)
            true
        } else {
            delay(DELAY_MILLIS * (max(1, DELAY_FACTOR * retryAttempts)))
            false
        }
    }

    private fun isValid(url: String?, validUntil: Date?, downloadId: Long?): Boolean {
        if (validUntil == null || url == null || downloadId == null) return false
        // Now represents the current time using the current locale and timezone
        val now = Calendar.getInstance().apply { time = Date() }
        val expires = Calendar.getInstance().apply { time = validUntil }
        return now.before(expires)
    }
}
