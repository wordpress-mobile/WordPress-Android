package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityRestClient
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityStore
@Inject constructor(private val mActivityRestClient: ActivityRestClient,
                    dispatcher: Dispatcher) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val payload = action.payload ?: return
        when (payload) {
            is FetchActivitiesPayload -> fetchActivities(payload)
            is FetchRewindStatePayload -> fetchActivitiesRewind(payload)
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, this.javaClass.name + ": onRegister")
    }

    private fun fetchActivities(fetchActivitiesPayload: FetchActivitiesPayload) {
        mActivityRestClient.fetchActivity(fetchActivitiesPayload.site,
                fetchActivitiesPayload.number,
                fetchActivitiesPayload.offset)
    }

    private fun fetchActivitiesRewind(fetchActivitiesRewindPayload: FetchRewindStatePayload) {
        mActivityRestClient.fetchActivityRewind(fetchActivitiesRewindPayload.site,
                fetchActivitiesRewindPayload.number,
                fetchActivitiesRewindPayload.offset)
    }
}

// Payloads
data class FetchActivitiesPayload(val site: SiteModel,
                                  val number: Int,
                                  val offset: Int) : Payload<BaseRequest.BaseNetworkError>()

data class FetchRewindStatePayload(val site: SiteModel,
                                   val number: Int,
                                   val offset: Int) : Payload<BaseRequest.BaseNetworkError>()

data class FetchedActivitiesPayload(val activityModelRespons: List<ActivityModel> = listOf(),
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

data class FetchedRewindStatePayload(val rewindStatusModelResponse: RewindStatusModel? = null,
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

data class ActivityError(var type: ActivityErrorType, var message: String? = null) : Store.OnChangedError

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

data class RewindStatusError(var type: RewindStatusErrorType, var message: String? = null) : Store.OnChangedError
