package org.wordpress.android.fluxc.store

import com.yarolegovich.wellsql.SelectQuery
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.ActivityAction
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

@Singleton
class ActivityLogStore
@Inject constructor(private val activityLogRestClient: ActivityLogRestClient,
                    private val activityLogSqlUtils: ActivityLogSqlUtils,
                    dispatcher: Dispatcher) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? ActivityAction ?: return

        when (actionType) {
            ActivityAction.FETCH_ACTIVITIES -> fetchActivities(action.payload as FetchActivitiesPayload)
            ActivityAction.FETCHED_ACTIVITIES -> storeActivityLog(action.payload as FetchedActivitiesPayload,
                    actionType)
            ActivityAction.FETCH_REWIND_STATE -> fetchActivitiesRewind(action.payload as FetchRewindStatePayload)
            ActivityAction.FETCHED_REWIND_STATE -> storeRewindState(action.payload as FetchedRewindStatePayload,
                    actionType)
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

    private fun fetchActivities(fetchActivitiesPayload: FetchActivitiesPayload) {
        activityLogRestClient.fetchActivity(fetchActivitiesPayload.site,
                fetchActivitiesPayload.number,
                fetchActivitiesPayload.offset)
    }

    private fun storeActivityLog(payload: FetchedActivitiesPayload, action: ActivityAction) {
        if (payload.activityLogModels.isNotEmpty()) {
            val rowsAffected = activityLogSqlUtils.insertOrUpdateActivities(payload.site, payload.activityLogModels)
            emitChange(OnActivitiesFetched(rowsAffected, action))
        } else if (payload.error != null) {
            emitChange(OnActivitiesFetched(payload.error, action))
        }
    }

    private fun storeRewindState(payload: FetchedRewindStatePayload, action: ActivityAction) {
        if (payload.rewindStatusModelResponse != null) {
            activityLogSqlUtils.insertOrUpdateRewindStatus(payload.site, payload.rewindStatusModelResponse)
            emitChange(OnRewindStatusFetched(action))
        } else if (payload.error != null) {
            emitChange(OnRewindStatusFetched(payload.error, action))
        }
    }

    private fun fetchActivitiesRewind(fetchActivitiesRewindPayload: FetchRewindStatePayload) {
        activityLogRestClient.fetchActivityRewind(fetchActivitiesRewindPayload.site)
    }

    // Actions
    data class OnActivitiesFetched(val rowsAffected: Int,
                                   var causeOfChange: ActivityAction) : Store.OnChanged<ActivityError>() {
        constructor(error: ActivityError, causeOfChange: ActivityAction) :
                this(rowsAffected = 0, causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    data class OnRewindStatusFetched(var causeOfChange: ActivityAction) : Store.OnChanged<RewindStatusError>() {
        constructor(error: RewindStatusError, causeOfChange: ActivityAction) :
                this(causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    // Payloads
    class FetchActivitiesPayload(val site: SiteModel,
                                 val number: Int,
                                 val offset: Int) : Payload<BaseRequest.BaseNetworkError>()

    class FetchRewindStatePayload(val site: SiteModel) : Payload<BaseRequest.BaseNetworkError>()

    class FetchedActivitiesPayload(val activityLogModels: List<ActivityLogModel> = listOf(),
                                   val site: SiteModel,
                                   val number: Int,
                                   val offset: Int) : Payload<ActivityError>() {
        constructor(error: ActivityError,
                    site: SiteModel,
                    number: Int,
                    offset: Int) : this(site = site, number = number, offset = offset) {
            this.error = error
        }
    }

    class FetchedRewindStatePayload(val rewindStatusModelResponse: RewindStatusModel? = null,
                                    val site: SiteModel) : Payload<RewindStatusError>() {
        constructor(error: RewindStatusError, site: SiteModel) : this(site = site) {
            this.error = error
        }
    }

    // Errors
    enum class ActivityErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE,
        MISSING_ACTIVITY_ID,
        MISSING_SUMMARY,
        MISSING_CONTENT_TEXT,
        MISSING_PUBLISHED_DATE
    }

    class ActivityError(var type: ActivityErrorType, var message: String? = null) : Store.OnChangedError

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
}
