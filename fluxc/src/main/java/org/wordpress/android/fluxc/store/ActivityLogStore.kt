package org.wordpress.android.fluxc.store

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
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityLogStore
@Inject constructor(private val activityLogRestClient: ActivityLogRestClient,
                    dispatcher: Dispatcher) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? ActivityAction ?: return

        when (actionType) {
            ActivityAction.FETCH_ACTIVITIES -> fetchActivities(action.payload as FetchActivitiesPayload)
            ActivityAction.FETCH_REWIND_STATE -> fetchActivitiesRewind(action.payload as FetchRewindStatePayload)
            else -> {
            }
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, this.javaClass.name + ": onRegister")
    }

    private fun fetchActivities(fetchActivitiesPayload: FetchActivitiesPayload) {
        activityLogRestClient.fetchActivity(fetchActivitiesPayload.site,
                fetchActivitiesPayload.number,
                fetchActivitiesPayload.offset)
    }

    private fun fetchActivitiesRewind(fetchActivitiesRewindPayload: FetchRewindStatePayload) {
        activityLogRestClient.fetchActivityRewind(fetchActivitiesRewindPayload.site,
                fetchActivitiesRewindPayload.number,
                fetchActivitiesRewindPayload.offset)
    }

    // Payloads
    class FetchActivitiesPayload(val site: SiteModel,
                                 val number: Int,
                                 val offset: Int) : Payload<BaseRequest.BaseNetworkError>()

    class FetchRewindStatePayload(val site: SiteModel,
                                  val number: Int,
                                  val offset: Int) : Payload<BaseRequest.BaseNetworkError>()

    class FetchedActivitiesPayload(val activityLogModelRespons: List<ActivityLogModel> = listOf(),
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
                                    val site: SiteModel,
                                    val number: Int,
                                    val offset: Int) : Payload<RewindStatusError>() {
        constructor(error: RewindStatusError,
                    site: SiteModel,
                    number: Int,
                    offset: Int) : this(site = site, number = number, offset = offset) {
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
