package org.wordpress.android.ui.jetpack.backup.download

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.BackupDownloadStatusModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadError
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadRequestTypes
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadStatusError
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchBackupDownloadStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.OnBackupDownload
import org.wordpress.android.modules.UI_SCOPE
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

// todo: annmarie - add tracking key - may be rewindId const val REWIND_ID_TRACKING_KEY = "rewind_id"

@Singleton
class BackupDownloadStatusService @Inject constructor(
    private val activityLogStore: ActivityLogStore,
    private val backupDownloadProgressChecker: BackupDownloadProgressChecker,
    @param:Named(UI_SCOPE) private val uiScope: CoroutineScope
) {
    private val mutableBackupDownloadAvailable = MutableLiveData<Boolean>()
    private val mutableBackupDownloadError = MutableLiveData<BackupDownloadError>()
    private val mutableBackupDownloadStatusFetchError = MutableLiveData<BackupDownloadStatusError>()
    private val mutableBackupDownloadProgress = MutableLiveData<BackupDownloadProgress>()
    private var site: SiteModel? = null
    private var activityLogModelItem: ActivityLogModel? = null
    private var backupDownloadProgressCheckerJob: Job? = null
    private var fetchBackupDownloadJob: Job? = null

    val preparingBackupDownloadActivityLogModel: ActivityLogModel?
        get() = activityLogModelItem

    val backupDownloadAvailable: LiveData<Boolean> = mutableBackupDownloadAvailable
    val backupDownloadError: LiveData<BackupDownloadError> = mutableBackupDownloadError
    val backupDownloadStatusFetchError: LiveData<BackupDownloadStatusError> = mutableBackupDownloadStatusFetchError
    val backupDownloadProgress: LiveData<BackupDownloadProgress> = mutableBackupDownloadProgress

    val isBackupDownloadInProgress: Boolean
        get() = backupDownloadProgress.value?.progress != null

    val isBackupDownloadAvailable: Boolean
        get() = backupDownloadAvailable.value == true

    fun backupDownload(rewindId: String, site: SiteModel, types: BackupDownloadRequestTypes) =
            uiScope.launch {
            // todo: annmarie - implement tracking here once naming has been decided
            // AnalyticsUtils.trackWithSiteDetails(
            //    AnalyticsTracker.Stat.ACTIVITY_LOG_PREPARE_BACKUP_DOWNLOAD_STARTED,
            //    site, mutableMapOf(REWIND_ID_TRACKING_KEY to rewindId as Any))

                updateBackupDownloadProgress(rewindId, 0)
                mutableBackupDownloadAvailable.value = false
                mutableBackupDownloadError.value = null

                val backupDownloadResult = activityLogStore.backupDownload(
                        BackupDownloadPayload(
                                site,
                                rewindId,
                                types
                        )
                )
                onBackupDownload(backupDownloadResult)
            }

    fun start(site: SiteModel) {
        if (this.site == null) {
            this.site = site
            requestStatusUpdate()
            reloadBackupDownloadStatus()
        }
    }

    fun stop() {
        backupDownloadProgressCheckerJob?.cancel()
        fetchBackupDownloadJob?.cancel()
        if (site != null) {
            site = null
        }
    }

    fun requestStatusUpdate() {
        site?.let {
            fetchBackupDownloadJob?.cancel()
            fetchBackupDownloadJob = uiScope.launch {
                val backupDownloadStatus = activityLogStore.fetchBackupDownloadState(
                        FetchBackupDownloadStatePayload(it)
                )
                onBackupDownloadStatusFetched(
                        backupDownloadStatus.error,
                        backupDownloadStatus.isError
                )
            }
        }
    }

    private fun reloadBackupDownloadStatus() {
        site?.let {
            val state = activityLogStore.getBackupDownloadStatusForSite(it)
            state?.let {
                updateBackupDownloadStatus(state)
            }
        }
    }

    private fun updateBackupDownloadStatus(backupDownloadStatus: BackupDownloadStatusModel?) {
        mutableBackupDownloadAvailable.value = backupDownloadStatus?.progress == null

        if (backupDownloadStatus != null) {
            val downloadId = backupDownloadStatus.downloadId
            if (backupDownloadProgressCheckerJob?.isActive != true) {
                site?.let {
                    backupDownloadProgressCheckerJob = uiScope.launch {
                        val backupDownloadStatusFetched = backupDownloadProgressChecker.startNow(
                                it,
                                downloadId
                        )
                        onBackupDownloadStatusFetched(
                                backupDownloadStatusFetched?.error,
                                backupDownloadStatusFetched?.isError == true
                        )
                    }
                }
            }
            updateBackupDownloadProgress(
                    backupDownloadStatus.rewindId,
                    backupDownloadStatus.progress
            )
            if (backupDownloadStatus.progress == null) {
                backupDownloadProgressCheckerJob?.cancel()
            }
        } else {
            mutableBackupDownloadProgress.setValue(null)
        }
    }

    private fun onBackupDownloadStatusFetched(
        backupDownloadStatusError: BackupDownloadStatusError?,
        isError: Boolean
    ) {
        mutableBackupDownloadStatusFetchError.value = backupDownloadStatusError
        if (isError) {
            backupDownloadProgressCheckerJob?.cancel()
        }
        reloadBackupDownloadStatus()
    }

    private fun onBackupDownload(event: OnBackupDownload) {
        mutableBackupDownloadError.value = event.error
        if (event.isError) {
            mutableBackupDownloadAvailable.value = true
            reloadBackupDownloadStatus()
            updateBackupDownloadProgress(
                    event.rewindId,
                    0,
                    event.error?.type?.toString()
            )
            return
        }
        site?.let {
            event.downloadId?.let { downloadId ->
                backupDownloadProgressCheckerJob = uiScope.launch {
                    val backupDownloadStatusFetched = backupDownloadProgressChecker.start(
                            it,
                            downloadId
                    )
                    onBackupDownloadStatusFetched(
                            backupDownloadStatusFetched?.error,
                            backupDownloadStatusFetched?.isError == true
                    )
                }
            }
        }
    }

    private fun updateBackupDownloadProgress(
        rewindId: String?,
        progress: Int?,
        backupDownloadError: String? = null
    ) {
        var activityItem = if (rewindId != null) activityLogStore.getActivityLogItemByRewindId(
                rewindId
        ) else null
        if (activityItem == null && activityLogModelItem != null && activityLogModelItem?.rewindID == rewindId) {
            activityItem = activityLogModelItem
        }
        if (activityItem != null) {
            activityLogModelItem = activityItem
        }
        val backupDownloadProgress = BackupDownloadProgress(
                activityItem,
                progress,
                backupDownloadError
        )
        mutableBackupDownloadProgress.value = backupDownloadProgress
    }

    data class BackupDownloadProgress(
        val activityLogItem: ActivityLogModel?,
        val progress: Int?,
        val failureReason: String? = null
    )
}
