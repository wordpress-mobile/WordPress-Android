package org.wordpress.android.fluxc.store

import com.yarolegovich.wellsql.SelectQuery
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.ActivityLogAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient
import org.wordpress.android.fluxc.persistence.ActivityLogSqlUtils
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

private const val ACTIVITY_LOG_PAGE_SIZE = 10

@Singleton
class ActivityLogStore
@Inject constructor(
    private val activityLogRestClient: ActivityLogRestClient,
    private val activityLogSqlUtils: ActivityLogSqlUtils,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? ActivityLogAction ?: return

        when (actionType) {
            ActivityLogAction.FETCH_ACTIVITIES -> fetchActivities(action.payload as FetchActivityLogPayload)
            ActivityLogAction.FETCHED_ACTIVITIES ->
                storeActivityLog(action.payload as FetchedActivityLogPayload, actionType)
            ActivityLogAction.FETCH_REWIND_STATE -> fetchActivitiesRewind(action.payload as FetchRewindStatePayload)
            ActivityLogAction.FETCHED_REWIND_STATE ->
                storeRewindState(action.payload as FetchedRewindStatePayload, actionType)
            ActivityLogAction.REWIND -> rewind(action.payload as RewindPayload)
            ActivityLogAction.REWIND_RESULT -> emitRewindResult(action.payload as RewindResultPayload, actionType)
        }
    }

    fun getActivityLogForSite(site: SiteModel, ascending: Boolean = true): List<ActivityLogModel> {
        val order = if (ascending) SelectQuery.ORDER_ASCENDING else SelectQuery.ORDER_DESCENDING
        return activityLogSqlUtils.getActivitiesForSite(site, order)
    }

    fun getRewindStatusForSite(site: SiteModel): RewindStatusModel? {
        return activityLogSqlUtils.getRewindStatusForSite(site)
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, this.javaClass.name + ": onRegister")
    }

    private fun fetchActivities(fetchActivityLogPayload: FetchActivityLogPayload) {
        var offset = 0
        if (fetchActivityLogPayload.loadMore) {
            offset = activityLogSqlUtils.getActivitiesForSite(
                    fetchActivityLogPayload.site,
                    SelectQuery.ORDER_ASCENDING
            ).size
        } else {
            activityLogSqlUtils.deleteActivityLog()
        }
        activityLogRestClient.fetchActivity(fetchActivityLogPayload.site, ACTIVITY_LOG_PAGE_SIZE, offset)
    }

    private fun rewind(rewindPayload: RewindPayload) {
        activityLogRestClient.rewind(rewindPayload.site, rewindPayload.rewindId)
    }

    private fun storeActivityLog(payload: FetchedActivityLogPayload, action: ActivityLogAction) {
        if (payload.error != null) {
            emitChange(OnActivityLogFetched(payload.error, action))
        } else {
            val rowsAffected = if (payload.activityLogModels.isNotEmpty())
                activityLogSqlUtils.insertOrUpdateActivities(payload.site, payload.activityLogModels)
            else 0
            val canLoadMore = payload.activityLogModels.isNotEmpty() &&
                    (payload.offset + payload.number) < payload.totalItems
            emitChange(OnActivityLogFetched(rowsAffected, canLoadMore, action))
        }
    }

    private fun storeRewindState(payload: FetchedRewindStatePayload, action: ActivityLogAction) {
        if (payload.error != null) {
            emitChange(OnRewindStatusFetched(payload.error, action))
        } else {
            if (payload.rewindStatusModelResponse != null) {
                activityLogSqlUtils.insertOrUpdateRewindStatus(payload.site, payload.rewindStatusModelResponse)
            }
            emitChange(OnRewindStatusFetched(action))
        }
    }

    private fun emitRewindResult(payload: RewindResultPayload, action: ActivityLogAction) {
        if (payload.error != null) {
            emitChange(OnRewind(payload.error, action))
        } else {
            emitChange(OnRewind(restoreId = payload.restoreId, causeOfChange = action))
        }
    }

    private fun fetchActivitiesRewind(fetchActivitiesRewindPayload: FetchRewindStatePayload) {
        activityLogRestClient.fetchActivityRewind(fetchActivitiesRewindPayload.site)
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
        val restoreId: String? = null,
        var causeOfChange: ActivityLogAction
    ) : Store.OnChanged<RewindError>() {
        constructor(error: RewindError, causeOfChange: ActivityLogAction) :
                this(restoreId = null, causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    // Payloads
    class FetchActivityLogPayload(
        val site: SiteModel,
        val loadMore: Boolean = false
    ) : Payload<BaseRequest.BaseNetworkError>()

    class FetchRewindStatePayload(val site: SiteModel) : Payload<BaseRequest.BaseNetworkError>()

    class RewindPayload(val site: SiteModel, val rewindId: String) : Payload<BaseRequest.BaseNetworkError>()

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

    class RewindResultPayload(val restoreId: String? = null, val site: SiteModel) : Payload<RewindError>() {
        constructor(error: RewindError, site: SiteModel) : this(site = site) {
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

    class ActivityError(var type: ActivityLogErrorType, var message: String? = null) : Store.OnChangedError

    enum class RewindStatusErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE,
        MISSING_STATE,
        INVALID_REWIND_STATE,
        MISSING_RESTORE_ID,
        MISSING_RESTORE_STATUS,
        INVALID_RESTORE_STATUS
    }

    class RewindStatusError(var type: RewindStatusErrorType, var message: String? = null) : Store.OnChangedError

    enum class RewindErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE,
        MISSING_STATE
    }
    class RewindError(var type: RewindErrorType, var message: String? = null) : Store.OnChangedError
}
