package org.wordpress.android.ui.jetpack.backup.download.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.DismissBackupDownloadPayload
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.max

class PostDismissBackupDownloadUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val activityLogStore: ActivityLogStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private val tag = javaClass.simpleName

    @Suppress("ComplexMethod", "LoopWithTooManyJumpStatements")
    suspend fun dismissBackupDownload(
        downloadId: Long,
        site: SiteModel
    ): Boolean = withContext(bgDispatcher) {
        var retryAttempts = 0
        var dismissed = false
        while (true) {
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                val retryAttemptsExceeded = handleError(retryAttempts++)
                if (retryAttemptsExceeded) break else continue
            }
            val result = activityLogStore.dismissBackupDownload(DismissBackupDownloadPayload(site, downloadId))
            if (result.isError) break
            dismissed = true
            break
        }
        dismissed
    }

    private suspend fun handleError(retryAttempts: Int) = if (retryAttempts >= MAX_RETRY) {
        AppLog.d(T.JETPACK_BACKUP, "$tag: Exceeded $MAX_RETRY retries while dismiss download backup file")
        true
    } else {
        delay(DELAY_MILLIS * max(1, DELAY_FACTOR * retryAttempts))
        false
    }
}
