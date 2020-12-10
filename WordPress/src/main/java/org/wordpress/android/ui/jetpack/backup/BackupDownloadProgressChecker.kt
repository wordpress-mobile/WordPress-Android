package org.wordpress.android.ui.jetpack.backup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCH_BACKUP_DOWNLOAD_STATE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchBackupDownloadStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.OnBackupDownloadStatusFetched
import org.wordpress.android.modules.DEFAULT_SCOPE
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

const val CHECK_DELAY_MILLIS = 10000L

@Singleton
class BackupDownloadProgressChecker @Inject constructor(
    private val activityLogStore: ActivityLogStore,
    @param:Named(DEFAULT_SCOPE) private val defaultScope: CoroutineScope
) {
    suspend fun startNow(site: SiteModel, downloadId: Long): OnBackupDownloadStatusFetched? {
        return start(site, downloadId, true)
    }

    suspend fun start(
        site: SiteModel,
        downloadId: Long,
        now: Boolean = false,
        checkDelay: Long = CHECK_DELAY_MILLIS
    ) = withContext(defaultScope.coroutineContext) {
        if (!now) {
            delay(checkDelay)
        }
        var result: OnBackupDownloadStatusFetched? = null
        while (coroutineContext.isActive) {
            val downloadStatusForSite = activityLogStore.getBackupDownloadStatusForSite(site)
            if (downloadStatusForSite != null && downloadStatusForSite.downloadId == downloadId) {
                if (downloadStatusForSite.progress == null) {
                    result = OnBackupDownloadStatusFetched(FETCH_BACKUP_DOWNLOAD_STATE)
                    break
                }
            }
            result = activityLogStore.fetchBackupDownloadState(
                    FetchBackupDownloadStatePayload(
                            site
                    )
            )
            if (result.isError) {
                break
            }
            delay(checkDelay)
        }
        return@withContext result
    }
}
