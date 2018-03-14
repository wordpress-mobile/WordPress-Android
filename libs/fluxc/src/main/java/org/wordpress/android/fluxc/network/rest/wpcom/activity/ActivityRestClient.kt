package org.wordpress.android.fluxc.network.rest.wpcom.activity

import android.content.Context
import android.util.Log
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ActivityActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.buildGetRequest
import org.wordpress.android.fluxc.network.rest.wpcom.activity.Activity.ActivityError
import org.wordpress.android.fluxc.network.rest.wpcom.activity.Activity.ActivityErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.activity.RewindStatus.RestoreStatus.*
import org.wordpress.android.fluxc.network.rest.wpcom.activity.RewindStatus.RewindStatusErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.ActivityStore
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class ActivityRestClient
@Inject
constructor(appContext: Context, dispatcher: Dispatcher, requestQueue: RequestQueue,
            accessToken: AccessToken, userAgent: UserAgent)
    : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    fun fetchActivity(site: SiteModel, number: Int, offset: Int) {
        val url = WPCOMREST.sites.site(site.siteId).activity.urlV2
        val pageNumber = offset / number + 1
        val params = mapOf("page" to pageNumber.toString(), "number" to number.toString())
        val request = buildGetRequest(
                url, params, ActivitiesResponse::class.java,
                {
                    val activities = it.current.orderedItems
                    val payload = buildActivityPayload(activities, site, number, offset)
                    mDispatcher.dispatch(ActivityActionBuilder.newFetchedActivitiesAction(payload))
                },
                {
                    val error = ActivityError(genericToError(it), it.message)
                    val payload = ActivityStore.FetchedActivitiesPayload(error, site, number, offset)
                    mDispatcher.dispatch(ActivityActionBuilder.newFetchedActivitiesAction(payload))
                })
        add(request)
    }

    fun fetchActivityRewind(site: SiteModel, number: Int, offset: Int) {
        val url = WPCOMREST.sites.site(site.siteId).rewind.urlV2
        val pageNumber = offset / number + 1
        val params = mapOf("page" to pageNumber.toString(), "number" to number.toString())
        val request = buildGetRequest(
                url, params, RewindStatusResponse::class.java,
                {
                    Log.d("activity_log", "Rewind: $it")
                    val payload = buildRewindStatusPayload(it, site, number, offset)
                    mDispatcher.dispatch(ActivityActionBuilder.newFetchedRewindStateAction(payload))
                },
                {
                    val error = RewindStatus.RewindStatusError(genericToRewindErrorType(it), it.message)
                    val payload = ActivityStore.FetchRewindStateResponsePayload(error, site, number, offset)
                    mDispatcher.dispatch(ActivityActionBuilder.newFetchedRewindStateAction(payload))
                })
        add(request)
    }

    private fun buildActivityPayload(activityResponses: List<ActivitiesResponse.ActivityResponse>, site: SiteModel, number: Int, offset: Int): ActivityStore.FetchedActivitiesPayload {
        var error: ActivityErrorType? = null

        val activities = activityResponses.mapNotNull {
            when {
                it.activity_id == null -> {
                    error = ActivityErrorType.MISSING_ACTIVITY_ID
                    null
                }
                it.summary == null -> {
                    error = ActivityErrorType.MISSING_SUMMARY
                    null
                }
                it.content?.text == null -> {
                    error = ActivityErrorType.MISSING_CONTENT_TEXT
                    null
                }
                it.published == null -> {
                    error = ActivityErrorType.MISSING_PUBLISHED_DATE
                    null
                }
                else -> Activity(it.activity_id,
                        it.summary,
                        it.content.text,
                        it.name,
                        it.type,
                        it.gridicon,
                        it.status,
                        it.is_rewindable,
                        it.rewind_id,
                        it.published,
                        it.is_discarded,
                        Activity.ActivityActor(it.actor.name,
                                it.actor.type,
                                it.actor.wpcom_user_id,
                                it.actor.icon?.url,
                                it.actor.role))
            }
        }
        error?.let {
            return ActivityStore.FetchedActivitiesPayload(ActivityError(it), site, number, offset)
        }
        return ActivityStore.FetchedActivitiesPayload(activities, site, number, offset)
    }

    private fun buildRewindStatusPayload(response: RewindStatusResponse, site: SiteModel, number: Int, offset: Int): ActivityStore.FetchRewindStateResponsePayload {
        if (response.state == null) {
            return error(site, number, offset, RewindStatusErrorType.MISSING_STATE)
        }
        var restoreStatus: RewindStatus.RestoreStatus? =
                if (response.restoreResponse != null) {
                    with(response.restoreResponse) {
                        if (this.rewind_id == null) {
                            return error(site, number, offset, RewindStatusErrorType.MISSING_RESTORE_ID)
                        }
                        if (this.status == null) {
                            return error(site, number, offset, RewindStatusErrorType.MISSING_RESTORE_STATUS)
                        }
                        val restoreStatus = Status.values().firstOrNull { it.value == this.status }
                                ?: return error(site, number, offset, RewindStatusErrorType.INVALID_RESTORE_STATUS)
                        RewindStatus.RestoreStatus(this.rewind_id,
                                restoreStatus,
                                this.progress,
                                this.message,
                                this.error_code,
                                this.reason)
                    }
                } else {
                    null
                }
        val rewindStatusState = RewindStatus.State.values().firstOrNull { it.value == response.state }
                ?: return error(site, number, offset, RewindStatusErrorType.INVALID_REWIND_STATE)
        val rewindStatus = RewindStatus(rewindStatusState, response.reason, restoreStatus)
        return ActivityStore.FetchRewindStateResponsePayload(rewindStatus, site, number, offset)
    }

    private fun error(site: SiteModel, number: Int, offset: Int, errorType: RewindStatusErrorType) =
            ActivityStore.FetchRewindStateResponsePayload(RewindStatus.RewindStatusError(errorType), site, number, offset)

    private fun genericToError(error: BaseRequest.BaseNetworkError): ActivityErrorType {
        var errorType = ActivityErrorType.GENERIC_ERROR
        if (error.isGeneric && error.type == BaseRequest.GenericErrorType.INVALID_RESPONSE) {
            errorType = ActivityErrorType.INVALID_RESPONSE
        }
        if (error is WPComGsonRequest.WPComGsonNetworkError) {
            if ("unauthorized" == error.apiError) {
                errorType = ActivityErrorType.AUTHORIZATION_REQUIRED
            }
        }
        return errorType
    }

    private fun genericToRewindErrorType(error: BaseRequest.BaseNetworkError): RewindStatusErrorType {
        var errorType = RewindStatusErrorType.GENERIC_ERROR
        if (error.isGeneric && error.type == BaseRequest.GenericErrorType.INVALID_RESPONSE) {
            errorType = RewindStatusErrorType.INVALID_RESPONSE
        }
        if (error is WPComGsonRequest.WPComGsonNetworkError) {
            if ("unauthorized" == error.apiError) {
                errorType = RewindStatusErrorType.AUTHORIZATION_REQUIRED
            }
        }
        return errorType
    }

    private data class ActivitiesResponse(val totalItems: Int?,
                                          val summary: String?,
                                          val current: Page) {
        data class Page(val orderedItems: List<ActivityResponse>)
        data class ActivityResponse(val summary: String?,
                                    val content: Content?,
                                    val name: String?,
                                    val actor: Actor,
                                    val type: String?,
                                    val published: Date?,
                                    val generator: Generator?,
                                    val is_rewindable: Boolean?,
                                    val rewind_id: Float?,
                                    val gridicon: String?,
                                    val status: String?,
                                    val activity_id: String?,
                                    val actvityObject: ActivityObject?,
                                    val is_discarded: Boolean?)

        data class Content(val text: String?)
        data class Actor(val type: String?,
                         val name: String?,
                         val external_user_id: Long?,
                         val wpcom_user_id: Long?,
                         val icon: Icon?,
                         val role: String?)

        data class Icon(val type: String?, val url: String?, val width: Int?, val height: Int?)
        data class Generator(val jetpack_version: Float?,
                             val blog_id: Long?)

        data class ActivityObject(val type: String,
                                  val name: String?,
                                  val external_user_id: Long?,
                                  val wpcom_user_id: Long?)
    }

    data class RewindStatusResponse(val reason: String,
                                    val state: String?,
                                    val last_updated: Date,
                                    val restoreResponse: RestoreStatusResponse?) {
        data class RestoreStatusResponse(val rewind_id: String?,
                                         val status: String?,
                                         val progress: Int = 0,
                                         val message: String?,
                                         val error_code: String?,
                                         val reason: String?)
    }
}

