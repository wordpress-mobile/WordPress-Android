package org.wordpress.android.fluxc.store

import android.annotation.SuppressLint
import com.yarolegovich.wellsql.SelectQuery
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.ActivityLogAction
import org.wordpress.android.fluxc.action.ActivityLogAction.BACKUP_DOWNLOAD
import org.wordpress.android.fluxc.action.ActivityLogAction.DISMISS_BACKUP_DOWNLOAD
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCH_ACTIVITIES
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCH_ACTIVITY_TYPES
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCH_BACKUP_DOWNLOAD_STATE
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCH_REWIND_STATE
import org.wordpress.android.fluxc.action.ActivityLogAction.REWIND
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.ActivityTypeModel
import org.wordpress.android.fluxc.model.activity.BackupDownloadStatusModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient
import org.wordpress.android.fluxc.persistence.ActivityLogSqlUtils
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private const val ACTIVITY_LOG_PAGE_SIZE = 100

@Singleton
class ActivityLogStore
@Inject constructor(
    private val activityLogRestClient: ActivityLogRestClient,
    private val activityLogSqlUtils: ActivityLogSqlUtils,
    private val coroutineEngine: CoroutineEngine,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? ActivityLogAction ?: return
        when (actionType) {
            FETCH_ACTIVITIES -> {
                coroutineEngine.launch(AppLog.T.API, this, "ActivityLog: On FETCH_ACTIVITIES") {
                    emitChange(fetchActivities(action.payload as FetchActivityLogPayload))
                }
            }
            FETCH_REWIND_STATE -> {
                coroutineEngine.launch(AppLog.T.API, this, "ActivityLog: On FETCH_REWIND_STATE") {
                    emitChange(fetchActivitiesRewind(action.payload as FetchRewindStatePayload))
                }
            }
            REWIND -> {
                coroutineEngine.launch(AppLog.T.API, this, "ActivityLog: On REWIND") {
                    emitChange(rewind(action.payload as RewindPayload))
                }
            }
            BACKUP_DOWNLOAD -> {
                coroutineEngine.launch(AppLog.T.API, this, "ActivityLog: On BACKUP_DOWNLOAD") {
                    emitChange(backupDownload(action.payload as BackupDownloadPayload))
                }
            }
            FETCH_BACKUP_DOWNLOAD_STATE -> {
                coroutineEngine.launch(AppLog.T.API, this, "ActivityLog: On FETCH_BACKUP_DOWNLOAD_STATE") {
                    emitChange(fetchBackupDownloadState(action.payload as FetchBackupDownloadStatePayload))
                }
            }
            FETCH_ACTIVITY_TYPES -> {
                coroutineEngine.launch(AppLog.T.API, this, "ActivityLog: On FETCH_ACTIVITY_TYPES") {
                    emitChange(fetchActivityTypes(action.payload as FetchActivityTypesPayload))
                }
            }
            DISMISS_BACKUP_DOWNLOAD -> {
                coroutineEngine.launch(AppLog.T.API, this, "ActivityLog: On DISMISS_BACKUP_DOWNLOAD") {
                    emitChange(dismissBackupDownload(action.payload as DismissBackupDownloadPayload))
                }
            }
        }
    }

    @SuppressLint("WrongConstant")
    fun getActivityLogForSite(
        site: SiteModel,
        ascending: Boolean = true,
        rewindableOnly: Boolean = false
    ): List<ActivityLogModel> {
        val order = if (ascending) SelectQuery.ORDER_ASCENDING else SelectQuery.ORDER_DESCENDING
        return if (rewindableOnly) {
            activityLogSqlUtils.getRewindableActivitiesForSite(site, order)
        } else {
            activityLogSqlUtils.getActivitiesForSite(site, order)
        }
    }

    fun getActivityLogItemByRewindId(rewindId: String): ActivityLogModel? {
        return activityLogSqlUtils.getActivityByRewindId(rewindId)
    }

    fun getActivityLogItemByActivityId(activityId: String): ActivityLogModel? {
        return activityLogSqlUtils.getActivityByActivityId(activityId)
    }

    fun getRewindStatusForSite(site: SiteModel): RewindStatusModel? {
        return activityLogSqlUtils.getRewindStatusForSite(site)
    }

    fun getBackupDownloadStatusForSite(site: SiteModel): BackupDownloadStatusModel? {
        return activityLogSqlUtils.getBackupDownloadStatusForSite(site)
    }

    fun clearActivityLogCache(site: SiteModel) {
        activityLogSqlUtils.deleteActivityLog(site)
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, this.javaClass.name + ": onRegister")
    }

    @SuppressLint("WrongConstant")
    suspend fun fetchActivities(fetchActivityLogPayload: FetchActivityLogPayload): OnActivityLogFetched {
        var offset = 0
        if (fetchActivityLogPayload.loadMore) {
            offset = activityLogSqlUtils.getActivitiesForSite(
                    fetchActivityLogPayload.site,
                    SelectQuery.ORDER_ASCENDING
            ).size
        }
        val payload = activityLogRestClient.fetchActivity(fetchActivityLogPayload, ACTIVITY_LOG_PAGE_SIZE, offset)
        return storeActivityLog(payload, FETCH_ACTIVITIES)
    }

    suspend fun fetchActivitiesRewind(fetchActivitiesRewindPayload: FetchRewindStatePayload): OnRewindStatusFetched {
        val payload = activityLogRestClient.fetchActivityRewind(fetchActivitiesRewindPayload.site)
        return storeRewindState(payload, FETCH_REWIND_STATE)
    }

    suspend fun rewind(rewindPayload: RewindPayload): OnRewind {
        val payload = activityLogRestClient.rewind(rewindPayload.site, rewindPayload.rewindId, rewindPayload.types)
        return emitRewindResult(payload, REWIND)
    }

    suspend fun backupDownload(backupDownloadPayload: BackupDownloadPayload): OnBackupDownload {
        val payload = activityLogRestClient.backupDownload(
                backupDownloadPayload.site,
                backupDownloadPayload.rewindId,
                backupDownloadPayload.types)
        return emitBackupDownloadResult(payload, BACKUP_DOWNLOAD)
    }

    suspend fun fetchBackupDownloadState(
        fetchBackupDownloadPayload: FetchBackupDownloadStatePayload
    ): OnBackupDownloadStatusFetched {
        val payload = activityLogRestClient.fetchBackupDownloadState(fetchBackupDownloadPayload.site)
        return storeBackupDownloadState(payload, FETCH_BACKUP_DOWNLOAD_STATE)
    }

    suspend fun fetchActivityTypes(fetchActivityTypesPayload: FetchActivityTypesPayload): OnActivityTypesFetched {
        val payload = activityLogRestClient.fetchActivityTypes(
                fetchActivityTypesPayload.remoteSiteId,
                fetchActivityTypesPayload.after,
                fetchActivityTypesPayload.before
        )
        return emitActivityTypesResult(payload, FETCH_ACTIVITY_TYPES)
    }

    suspend fun dismissBackupDownload(backupDownloadPayload: DismissBackupDownloadPayload): OnDismissBackupDownload {
        val payload = activityLogRestClient.dismissBackupDownload(
                backupDownloadPayload.site,
                backupDownloadPayload.downloadId)
        return emitDismissBackupDownloadResult(payload, DISMISS_BACKUP_DOWNLOAD)
    }

    private fun storeActivityLog(payload: FetchedActivityLogPayload, action: ActivityLogAction): OnActivityLogFetched {
        return if (payload.error != null) {
            OnActivityLogFetched(payload.error, action)
        } else {
            var rowsAffected = 0
            if (payload.offset == 0) {
                rowsAffected += activityLogSqlUtils.deleteActivityLog(payload.site)
            }
            if (payload.activityLogModels.isNotEmpty()) {
                rowsAffected += activityLogSqlUtils.insertOrUpdateActivities(payload.site, payload.activityLogModels)
            }
            val canLoadMore = payload.activityLogModels.isNotEmpty() &&
                    (payload.offset + payload.number) < payload.totalItems
            OnActivityLogFetched(rowsAffected, canLoadMore, action)
        }
    }

    private fun storeRewindState(payload: FetchedRewindStatePayload, action: ActivityLogAction): OnRewindStatusFetched {
        return if (payload.error != null) {
            OnRewindStatusFetched(payload.error, action)
        } else {
            if (payload.rewindStatusModelResponse != null) {
                activityLogSqlUtils.replaceRewindStatus(payload.site, payload.rewindStatusModelResponse)
            } else {
                activityLogSqlUtils.deleteRewindStatus(payload.site)
            }
            OnRewindStatusFetched(action)
        }
    }

    private fun storeBackupDownloadState(payload: FetchedBackupDownloadStatePayload, action: ActivityLogAction):
            OnBackupDownloadStatusFetched {
        return if (payload.error != null) {
            OnBackupDownloadStatusFetched(payload.error, action)
        } else {
            if (payload.backupDownloadStatusModelResponse != null) {
                activityLogSqlUtils.replaceBackupDownloadStatus(payload.site, payload.backupDownloadStatusModelResponse)
            } else {
                activityLogSqlUtils.deleteBackupDownloadStatus(payload.site)
            }
            OnBackupDownloadStatusFetched(action)
        }
    }

    private fun emitRewindResult(payload: RewindResultPayload, action: ActivityLogAction): OnRewind {
        return if (payload.error != null) {
            OnRewind(payload.rewindId, payload.error, action)
        } else {
            OnRewind(rewindId = payload.rewindId, restoreId = payload.restoreId, causeOfChange = action)
        }
    }

    private fun emitBackupDownloadResult(
        payload: BackupDownloadResultPayload,
        action: ActivityLogAction
    ): OnBackupDownload {
        return if (payload.error != null) {
            OnBackupDownload(payload.rewindId, payload.error, action)
        } else {
            OnBackupDownload(
                    rewindId = payload.rewindId,
                    downloadId = payload.downloadId,
                    backupPoint = payload.backupPoint,
                    startedAt = payload.startedAt,
                    progress = payload.progress,
                    causeOfChange = action)
        }
    }

    private fun emitActivityTypesResult(
        payload: FetchedActivityTypesResultPayload,
        action: ActivityLogAction
    ): OnActivityTypesFetched {
        return if (payload.error != null) {
            OnActivityTypesFetched(payload.remoteSiteId, payload.error, action)
        } else {
            OnActivityTypesFetched(
                    causeOfChange = action,
                    remoteSiteId = payload.remoteSiteId,
                    activityTypeModels = payload.activityTypeModels,
                    totalItems = payload.totalItems
            )
        }
    }

    private fun emitDismissBackupDownloadResult(
        payload: DismissBackupDownloadResultPayload,
        action: ActivityLogAction
    ): OnDismissBackupDownload {
        return if (payload.error != null) {
            OnDismissBackupDownload(payload.downloadId, payload.error, action)
        } else {
            OnDismissBackupDownload(
                    downloadId = payload.downloadId,
                    isDismissed = payload.isDismissed,
                    causeOfChange = action
            )
        }
    }

    // Actions
    data class OnActivityLogFetched(
        val rowsAffected: Int,
        val canLoadMore: Boolean,
        val causeOfChange: ActivityLogAction
    ) : Store.OnChanged<ActivityError>() {
        constructor(error: ActivityError, causeOfChange: ActivityLogAction) :
                this(rowsAffected = 0, canLoadMore = true, causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    data class OnRewindStatusFetched(
        val causeOfChange: ActivityLogAction
    ) : Store.OnChanged<RewindStatusError>() {
        constructor(error: RewindStatusError, causeOfChange: ActivityLogAction) :
                this(causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    data class OnRewind(
        val rewindId: String,
        val restoreId: Long? = null,
        val causeOfChange: ActivityLogAction
    ) : Store.OnChanged<RewindError>() {
        constructor(rewindId: String, error: RewindError, causeOfChange: ActivityLogAction) :
                this(rewindId = rewindId, restoreId = null, causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    data class OnBackupDownload(
        val rewindId: String,
        val downloadId: Long? = null,
        val backupPoint: String? = null,
        val startedAt: String? = null,
        val progress: Int = 0,
        val causeOfChange: ActivityLogAction
    ) : Store.OnChanged<BackupDownloadError>() {
        constructor(rewindId: String, error: BackupDownloadError, causeOfChange: ActivityLogAction) :
                this(rewindId = rewindId, downloadId = null, causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    data class OnBackupDownloadStatusFetched(
        val causeOfChange: ActivityLogAction
    ) : Store.OnChanged<BackupDownloadStatusError>() {
        constructor(error: BackupDownloadStatusError, causeOfChange: ActivityLogAction) :
                this(causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    data class OnActivityTypesFetched(
        val causeOfChange: ActivityLogAction,
        val remoteSiteId: Long,
        val activityTypeModels: List<ActivityTypeModel>,
        val totalItems: Int = 0
    ) : Store.OnChanged<ActivityTypesError>() {
        constructor(
            remoteSiteId: Long,
            error: ActivityTypesError,
            causeOfChange: ActivityLogAction
        ) : this(remoteSiteId = remoteSiteId, causeOfChange = causeOfChange, activityTypeModels = listOf()) {
            this.error = error
        }
    }

    data class OnDismissBackupDownload(
        val downloadId: Long,
        val isDismissed: Boolean = false,
        val causeOfChange: ActivityLogAction
    ) : Store.OnChanged<DismissBackupDownloadError>() {
        constructor(downloadId: Long, error: DismissBackupDownloadError, causeOfChange: ActivityLogAction) :
                this(downloadId = downloadId, causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    // Payloads
    class FetchActivityLogPayload(
        val site: SiteModel,
        val loadMore: Boolean = false,
        val after: Date? = null,
        val before: Date? = null,
        val groups: List<String> = listOf()
    ) : Payload<BaseRequest.BaseNetworkError>()

    class FetchRewindStatePayload(val site: SiteModel) : Payload<BaseRequest.BaseNetworkError>()

    class RewindPayload(
        val site: SiteModel,
        val rewindId: String,
        val types: RewindRequestTypes? = null
    ) : Payload<BaseRequest.BaseNetworkError>()

    class FetchedActivityLogPayload(
        val activityLogModels: List<ActivityLogModel> = listOf(),
        val site: SiteModel,
        val totalItems: Int,
        val number: Int,
        val offset: Int
    ) : Payload<ActivityError>() {
        constructor(
            error: ActivityError,
            site: SiteModel,
            totalItems: Int = 0,
            number: Int,
            offset: Int
        ) : this(site = site, totalItems = totalItems, number = number, offset = offset) {
            this.error = error
        }
    }

    class FetchedRewindStatePayload(
        val rewindStatusModelResponse: RewindStatusModel? = null,
        val site: SiteModel
    ) : Payload<RewindStatusError>() {
        constructor(error: RewindStatusError, site: SiteModel) : this(site = site) {
            this.error = error
        }
    }

    class RewindResultPayload(
        val rewindId: String,
        val restoreId: Long? = null,
        val site: SiteModel
    ) : Payload<RewindError>() {
        constructor(error: RewindError, rewindId: String, site: SiteModel) : this(rewindId = rewindId, site = site) {
            this.error = error
        }
    }

    class BackupDownloadPayload(
        val site: SiteModel,
        val rewindId: String,
        val types: BackupDownloadRequestTypes
    ) : Payload<BaseRequest.BaseNetworkError>()

    class BackupDownloadResultPayload(
        val rewindId: String,
        val downloadId: Long? = null,
        val backupPoint: String? = null,
        val startedAt: String? = null,
        val progress: Int = 0,
        val site: SiteModel
    ) : Payload<BackupDownloadError>() {
        constructor(error: BackupDownloadError, rewindId: String, site: SiteModel) :
                this(rewindId = rewindId, site = site) {
            this.error = error
        }
    }

    class FetchBackupDownloadStatePayload(val site: SiteModel) : Payload<BaseRequest.BaseNetworkError>()

    class FetchedBackupDownloadStatePayload(
        val backupDownloadStatusModelResponse: BackupDownloadStatusModel? = null,
        val site: SiteModel
    ) : Payload<BackupDownloadStatusError>() {
        constructor(error: BackupDownloadStatusError, site: SiteModel) : this(site = site) {
            this.error = error
        }
    }

    class FetchActivityTypesPayload(
        val remoteSiteId: Long,
        val after: Date?,
        val before: Date?
    ) : Payload<BaseRequest.BaseNetworkError>()

    class FetchedActivityTypesResultPayload(
        val remoteSiteId: Long,
        val activityTypeModels: List<ActivityTypeModel>,
        val totalItems: Int = 0
    ) : Payload<ActivityTypesError>() {
        constructor(error: ActivityTypesError, remoteSiteId: Long) : this(
                remoteSiteId = remoteSiteId,
                activityTypeModels = listOf()
        ) {
            this.error = error
        }
    }

    data class RewindRequestTypes(
        val themes: Boolean,
        val plugins: Boolean,
        val uploads: Boolean,
        val sqls: Boolean,
        val roots: Boolean,
        val contents: Boolean
    )

    data class BackupDownloadRequestTypes(
        val themes: Boolean,
        val plugins: Boolean,
        val uploads: Boolean,
        val sqls: Boolean,
        val roots: Boolean,
        val contents: Boolean
    )

    class DismissBackupDownloadPayload(
        val site: SiteModel,
        val downloadId: Long
    ) : Payload<BaseRequest.BaseNetworkError>()

    class DismissBackupDownloadResultPayload(
        val siteId: Long,
        val downloadId: Long,
        val isDismissed: Boolean = false
    ) : Payload<DismissBackupDownloadError>() {
        constructor(
            error: DismissBackupDownloadError,
            siteId: Long,
            downloadId: Long
        ) : this(siteId = siteId, downloadId = downloadId) {
            this.error = error
        }
    }

    // Errors
    enum class ActivityLogErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE,
        MISSING_ACTIVITY_ID,
        MISSING_SUMMARY,
        MISSING_CONTENT_TEXT,
        MISSING_PUBLISHED_DATE
    }

    class ActivityError(var type: ActivityLogErrorType, var message: String? = null) : OnChangedError

    enum class RewindStatusErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE,
        INVALID_REWIND_STATE,
        MISSING_REWIND_ID,
        MISSING_RESTORE_ID
    }

    class RewindStatusError(var type: RewindStatusErrorType, var message: String? = null) : OnChangedError

    enum class RewindErrorType {
        GENERIC_ERROR,
        API_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE,
        MISSING_STATE
    }

    class RewindError(var type: RewindErrorType, var message: String? = null) : OnChangedError

    enum class BackupDownloadErrorType {
        GENERIC_ERROR,
        API_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE
    }

    class BackupDownloadError(var type: BackupDownloadErrorType, var message: String? = null) : OnChangedError

    enum class BackupDownloadStatusErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE
    }

    class BackupDownloadStatusError(var type: BackupDownloadStatusErrorType, var message: String? = null) :
            OnChangedError

    enum class ActivityTypesErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE
    }

    class ActivityTypesError(var type: ActivityTypesErrorType, var message: String? = null) : OnChangedError

    class DismissBackupDownloadError(var type: DismissBackupDownloadErrorType, var message: String? = null) :
            OnChangedError

    enum class DismissBackupDownloadErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE
    }
}
