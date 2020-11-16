package org.wordpress.android.fluxc.store

import android.annotation.SuppressLint
import com.yarolegovich.wellsql.SelectQuery
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.ActivityLogAction
import org.wordpress.android.fluxc.action.ActivityLogAction.DOWNLOAD
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCH_ACTIVITIES
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCH_DOWNLOAD_STATE
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCH_REWIND_STATE
import org.wordpress.android.fluxc.action.ActivityLogAction.REWIND
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.DownloadStatusModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient
import org.wordpress.android.fluxc.persistence.ActivityLogSqlUtils
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private const val ACTIVITY_LOG_PAGE_SIZE = 10

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
            DOWNLOAD -> {
                coroutineEngine.launch(AppLog.T.API, this, "ActivityLog: On DOWNLOAD") {
                    emitChange(download(action.payload as DownloadPayload))
                }
            }
            FETCH_DOWNLOAD_STATE -> {
                coroutineEngine.launch(AppLog.T.API, this, "ActivityLog: On FETCH_DOWNLOAD_STATE") {
                    emitChange(fetchActivitiesDownload(action.payload as FetchDownloadStatePayload))
                }
            }
        }
    }

    @SuppressLint("WrongConstant")
    fun getActivityLogForSite(site: SiteModel, ascending: Boolean = true): List<ActivityLogModel> {
        val order = if (ascending) SelectQuery.ORDER_ASCENDING else SelectQuery.ORDER_DESCENDING
        return activityLogSqlUtils.getActivitiesForSite(site, order)
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

    suspend fun download(downloadPayload: DownloadPayload): OnDownload {
        val payload =
                activityLogRestClient.download(downloadPayload.site, downloadPayload.rewindId, downloadPayload.types)
        return emitDownloadResult(payload, DOWNLOAD)
    }

    suspend fun fetchActivitiesDownload(
        fetchActivitiesDownloadPayload: FetchDownloadStatePayload
    ): OnDownloadStatusFetched {
        val payload = activityLogRestClient.fetchActivityDownload(fetchActivitiesDownloadPayload.site)
        return storeDownloadState(payload, FETCH_DOWNLOAD_STATE)
    }

    private fun storeActivityLog(payload: FetchedActivityLogPayload, action: ActivityLogAction): OnActivityLogFetched {
        return if (payload.error != null) {
            OnActivityLogFetched(payload.error, action)
        } else {
            if (payload.offset == 0) {
                activityLogSqlUtils.deleteActivityLog()
            }
            val rowsAffected = if (payload.activityLogModels.isNotEmpty())
                activityLogSqlUtils.insertOrUpdateActivities(payload.site, payload.activityLogModels)
            else 0
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
            }
            OnRewindStatusFetched(action)
        }
    }

    private fun storeDownloadState(payload: FetchedDownloadStatePayload, action: ActivityLogAction):
            OnDownloadStatusFetched {
        return if (payload.error != null) {
            OnDownloadStatusFetched(payload.error, action)
        } else {
            if (payload.downloadStatusModelResponse != null) {
                activityLogSqlUtils.replaceDownloadStatus(payload.site, payload.downloadStatusModelResponse)
            }
            OnDownloadStatusFetched(action)
        }
    }

    private fun emitRewindResult(payload: RewindResultPayload, action: ActivityLogAction): OnRewind {
        return if (payload.error != null) {
            OnRewind(payload.rewindId, payload.error, action)
        } else {
            OnRewind(rewindId = payload.rewindId, restoreId = payload.restoreId, causeOfChange = action)
        }
    }

    private fun emitDownloadResult(payload: DownloadResultPayload, action: ActivityLogAction): OnDownload {
        return if (payload.error != null) {
            OnDownload(payload.rewindId, payload.error, action)
        } else {
            OnDownload(
                    rewindId = payload.rewindId,
                    downloadId = payload.downloadId,
                    backupPoint = payload.backupPoint,
                    startedAt = payload.startedAt,
                    progress = payload.progress,
                    causeOfChange = action)
        }
    }

    // Actions
    data class OnActivityLogFetched(
        val rowsAffected: Int,
        val canLoadMore: Boolean,
        var causeOfChange: ActivityLogAction
    ) : Store.OnChanged<ActivityError>() {
        constructor(error: ActivityError, causeOfChange: ActivityLogAction) :
                this(rowsAffected = 0, canLoadMore = true, causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    data class OnRewindStatusFetched(var causeOfChange: ActivityLogAction) : Store.OnChanged<RewindStatusError>() {
        constructor(error: RewindStatusError, causeOfChange: ActivityLogAction) :
                this(causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    data class OnRewind(
        val rewindId: String,
        val restoreId: Long? = null,
        var causeOfChange: ActivityLogAction
    ) : Store.OnChanged<RewindError>() {
        constructor(rewindId: String, error: RewindError, causeOfChange: ActivityLogAction) :
                this(rewindId = rewindId, restoreId = null, causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    data class OnDownload(
        val rewindId: String,
        val downloadId: Long? = null,
        val backupPoint: String? = null,
        val startedAt: String? = null,
        val progress: Int = 0,
        var causeOfChange: ActivityLogAction
    ) : Store.OnChanged<DownloadError>() {
        constructor(rewindId: String, error: DownloadError, causeOfChange: ActivityLogAction) :
                this(rewindId = rewindId, downloadId = null, causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    data class OnDownloadStatusFetched(var causeOfChange: ActivityLogAction) :
            Store.OnChanged<DownloadStatusError>() {
        constructor(error: DownloadStatusError, causeOfChange: ActivityLogAction) :
                this(causeOfChange = causeOfChange) {
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

    class DownloadPayload(
        val site: SiteModel,
        val rewindId: String,
        val types: DownloadRequestTypes
    ) : Payload<BaseRequest.BaseNetworkError>()

    class DownloadResultPayload(
        val rewindId: String,
        val downloadId: Long? = null,
        val backupPoint: String? = null,
        val startedAt: String? = null,
        val progress: Int = 0,
        val site: SiteModel
    ) : Payload<DownloadError>() {
        constructor(error: DownloadError, rewindId: String, site: SiteModel) : this(rewindId = rewindId, site = site) {
            this.error = error
        }
    }

    class FetchDownloadStatePayload(val site: SiteModel) : Payload<BaseRequest.BaseNetworkError>()

    class FetchedDownloadStatePayload(
        val downloadStatusModelResponse: DownloadStatusModel? = null,
        val site: SiteModel
    ) : Payload<DownloadStatusError>() {
        constructor(error: DownloadStatusError, site: SiteModel) : this(site = site) {
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

    data class DownloadRequestTypes(
        val themes: Boolean,
        val plugins: Boolean,
        val uploads: Boolean,
        val sqls: Boolean,
        val roots: Boolean,
        val contents: Boolean
    )

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

    enum class DownloadErrorType {
        GENERIC_ERROR,
        API_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE
    }

    class DownloadError(var type: DownloadErrorType, var message: String? = null) : OnChangedError

    enum class DownloadStatusErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE
    }

    class DownloadStatusError(var type: DownloadStatusErrorType, var message: String? = null) :
            OnChangedError
}
