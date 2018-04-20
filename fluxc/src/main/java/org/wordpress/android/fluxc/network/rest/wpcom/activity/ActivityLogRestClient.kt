package org.wordpress.android.fluxc.network.rest.wpcom.activity

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ActivityLogActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.ActivityError
import org.wordpress.android.fluxc.store.ActivityLogStore.ActivityLogErrorType
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedActivityLogPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedRewindStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindErrorType
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindStatusError
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindStatusErrorType
import java.util.Date
import javax.inject.Singleton

@Singleton
class ActivityLogRestClient
constructor(
    private val dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) :
        BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    fun fetchActivity(site: SiteModel, number: Int, offset: Int) {
        val url = WPCOMV2.sites.site(site.siteId).activity.url
        val pageNumber = offset / number + 1
        val params = mapOf("page" to pageNumber.toString(), "number" to number.toString())
        val request = wpComGsonRequestBuilder.buildGetRequest(
                url, params, ActivitiesResponse::class.java,
                { response ->
                    val activities = response.current?.orderedItems ?: listOf()
                    val totalItems = response.totalItems ?: 0
                    val payload = buildActivityPayload(activities, site, totalItems, number, offset)
                    dispatcher.dispatch(ActivityLogActionBuilder.newFetchedActivitiesAction(payload))
                },
                { networkError ->
                    val errorType = genericToError(
                            networkError,
                            ActivityLogErrorType.GENERIC_ERROR,
                            ActivityLogErrorType.INVALID_RESPONSE,
                            ActivityLogErrorType.AUTHORIZATION_REQUIRED
                    )
                    val error = ActivityError(errorType, networkError.message)
                    val payload = FetchedActivityLogPayload(error, site, number = number, offset = offset)
                    dispatcher.dispatch(ActivityLogActionBuilder.newFetchedActivitiesAction(payload))
                })
        add(request)
    }

    fun fetchActivityRewind(site: SiteModel) {
        val url = WPCOMV2.sites.site(site.siteId).rewind.url
        val request = wpComGsonRequestBuilder.buildGetRequest(
                url, mapOf(), RewindStatusResponse::class.java,
                { response ->
                    val payload = buildRewindStatusPayload(response, site)
                    dispatcher.dispatch(ActivityLogActionBuilder.newFetchedRewindStateAction(payload))
                },
                { networkError ->
                    val errorType = genericToError(
                            networkError,
                            RewindStatusErrorType.GENERIC_ERROR,
                            RewindStatusErrorType.INVALID_RESPONSE,
                            RewindStatusErrorType.AUTHORIZATION_REQUIRED
                    )
                    val error = RewindStatusError(errorType, networkError.message)
                    val payload = FetchedRewindStatePayload(error, site)
                    dispatcher.dispatch(ActivityLogActionBuilder.newFetchedRewindStateAction(payload))
                })
        add(request)
    }

    fun rewind(site: SiteModel, rewindId: String) {
        val url = WPCOMREST.activity_log.site(site.siteId).rewind.to.rewind(rewindId).urlV1
        val request = wpComGsonRequestBuilder.buildPostRequest(url, mapOf(), RewindResponse::class.java,
                { response ->
                    val payload = ActivityLogStore.RewindResultPayload(response.restore_id, site)
                    dispatcher.dispatch(ActivityLogActionBuilder.newRewindResultAction(payload))
                },
                { networkError ->
                    val error = ActivityLogStore.RewindError(genericToError(networkError,
                            RewindErrorType.GENERIC_ERROR,
                            RewindErrorType.INVALID_RESPONSE,
                            RewindErrorType.AUTHORIZATION_REQUIRED), networkError.message)
                    val payload = ActivityLogStore.RewindResultPayload(error, site)
                    dispatcher.dispatch(ActivityLogActionBuilder.newRewindResultAction(payload))
                })
        add(request)
    }

    private fun buildActivityPayload(
        activityResponses: List<ActivitiesResponse.ActivityResponse>,
        site: SiteModel,
        totalItems: Int,
        number: Int,
        offset: Int
    ): FetchedActivityLogPayload {
        var error: ActivityLogErrorType? = null

        val activities = activityResponses.mapNotNull {
            when {
                it.activity_id == null -> {
                    error = ActivityLogErrorType.MISSING_ACTIVITY_ID
                    null
                }
                it.summary == null -> {
                    error = ActivityLogErrorType.MISSING_SUMMARY
                    null
                }
                it.content?.text == null -> {
                    error = ActivityLogErrorType.MISSING_CONTENT_TEXT
                    null
                }
                it.published == null -> {
                    error = ActivityLogErrorType.MISSING_PUBLISHED_DATE
                    null
                }
                else -> {
                    ActivityLogModel(
                            activityID = it.activity_id,
                            summary = it.summary,
                            text = it.content.text,
                            name = it.name,
                            type = it.type,
                            gridicon = it.gridicon,
                            status = it.status,
                            rewindable = it.is_rewindable,
                            rewindID = it.rewind_id,
                            published = it.published,
                            actor = it.actor?.let {
                                ActivityLogModel.ActivityActor(
                                        it.name,
                                        it.type,
                                        it.wpcom_user_id,
                                        it.icon?.url,
                                        it.role
                                )
                            }
                    )
                }
            }
        }
        error?.let {
            return FetchedActivityLogPayload(ActivityError(it), site, totalItems, number, offset)
        }
        return FetchedActivityLogPayload(activities, site, totalItems, number, offset)
    }

    private fun buildRewindStatusPayload(response: RewindStatusResponse, site: SiteModel):
            FetchedRewindStatePayload {
        val rewindStatusValue = response.state ?: return buildErrorPayload(site, RewindStatusErrorType.MISSING_STATE)
        val rewindStatus = RewindStatusModel.State.fromValue(rewindStatusValue)
                ?: return buildErrorPayload(site, RewindStatusErrorType.INVALID_REWIND_STATE)
        val restoreStatusModel = response.restoreResponse?.let {
            val rewindId = it.rewind_id
                    ?: return buildErrorPayload(site, RewindStatusErrorType.MISSING_RESTORE_ID)
            val restoreStatusValue = it.status
                    ?: return buildErrorPayload(site, RewindStatusErrorType.MISSING_RESTORE_STATUS)
            val restoreStatus = RewindStatusModel.RestoreStatus.Status.fromValue(restoreStatusValue)
                    ?: return buildErrorPayload(site, RewindStatusErrorType.INVALID_RESTORE_STATUS)
            RewindStatusModel.RestoreStatus(
                    id = rewindId,
                    status = restoreStatus,
                    progress = it.progress,
                    message = it.message,
                    errorCode = it.error_code,
                    failureReason = it.reason
            )
        }

        val rewindStatusModel = RewindStatusModel(
                state = rewindStatus,
                reason = response.reason,
                restore = restoreStatusModel
        )
        return FetchedRewindStatePayload(rewindStatusModel, site)
    }

    private fun buildErrorPayload(site: SiteModel, errorType: RewindStatusErrorType) =
            FetchedRewindStatePayload(RewindStatusError(errorType), site)

    private fun <T> genericToError(
        error: WPComGsonNetworkError,
        genericError: T,
        invalidResponse: T,
        authorizationRequired: T
    ): T {
        var errorType = genericError
        if (error.isGeneric && error.type == BaseRequest.GenericErrorType.INVALID_RESPONSE) {
            errorType = invalidResponse
        }
        if ("unauthorized" == error.apiError) {
            errorType = authorizationRequired
        }
        return errorType
    }

    class ActivitiesResponse(
        val totalItems: Int?,
        val summary: String?,
        val current: Page?
    ) {
        class Page(val orderedItems: List<ActivityResponse>)
        data class ActivityResponse(
            val summary: String?,
            val content: Content?,
            val name: String?,
            val actor: Actor?,
            val type: String?,
            val published: Date?,
            val generator: Generator?,
            val is_rewindable: Boolean?,
            val rewind_id: String?,
            val gridicon: String?,
            val status: String?,
            val activity_id: String?
        )

        class Content(val text: String?)
        class Actor(
            val type: String?,
            val name: String?,
            val external_user_id: Long?,
            val wpcom_user_id: Long?,
            val icon: Icon?,
            val role: String?
        )

        class Icon(val type: String?, val url: String?, val width: Int?, val height: Int?)
        class Generator(val jetpack_version: Float?, val blog_id: Long?)
    }

    data class RewindStatusResponse(
        val reason: String,
        val state: String?,
        val last_updated: Date,
        val restoreResponse: RestoreStatusResponse?
    ) {
        data class RestoreStatusResponse(
            val rewind_id: String?,
            val status: String?,
            val progress: Int = 0,
            val message: String?,
            val error_code: String?,
            val reason: String?
        )
    }

    class RewindResponse(val restore_id: String)
}
