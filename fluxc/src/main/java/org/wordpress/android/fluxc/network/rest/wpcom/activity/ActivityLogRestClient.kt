package org.wordpress.android.fluxc.network.rest.wpcom.activity

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ActivityActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.store.ActivityLogStore.ActivityError
import org.wordpress.android.fluxc.store.ActivityLogStore.ActivityErrorType
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedActivitiesPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedRewindStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindStatusError
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindStatusErrorType
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class ActivityLogRestClient
@Inject constructor(private val mDispatcher: Dispatcher,
                    private val mRestClient: BaseWPComRestClient,
                    private val mWPComGsonRequestBuilder: WPComGsonRequestBuilder) {
    fun fetchActivity(site: SiteModel, number: Int, offset: Int) {
        val url = WPCOMV2.sites.site(site.siteId).activity.url
        val pageNumber = offset / number + 1
        val params = mapOf("page" to pageNumber.toString(), "number" to number.toString())
        val request = mWPComGsonRequestBuilder.buildGetRequest(
                url, params, ActivitiesResponse::class.java,
                { response ->
                    val activities = response.current.orderedItems
                    val payload = buildActivityPayload(activities, site, number, offset)
                    mDispatcher.dispatch(ActivityActionBuilder.newFetchedActivitiesAction(payload))
                },
                { networkError ->
                    val error = ActivityError(genericToError(networkError,
                            ActivityErrorType.GENERIC_ERROR,
                            ActivityErrorType.INVALID_RESPONSE,
                            ActivityErrorType.AUTHORIZATION_REQUIRED), networkError.message)
                    val payload = FetchedActivitiesPayload(error, site, number, offset)
                    mDispatcher.dispatch(ActivityActionBuilder.newFetchedActivitiesAction(payload))
                })
        mRestClient.add(request)
    }

    fun fetchActivityRewind(site: SiteModel) {
        val url = WPCOMV2.sites.site(site.siteId).rewind.url
        val request = mWPComGsonRequestBuilder.buildGetRequest(
                url, mapOf(), RewindStatusResponse::class.java,
                {
                    val payload = buildRewindStatusPayload(it, site)
                    mDispatcher.dispatch(ActivityActionBuilder.newFetchedRewindStateAction(payload))
                },
                {
                    val error = RewindStatusError(genericToError(it,
                            RewindStatusErrorType.GENERIC_ERROR,
                            RewindStatusErrorType.INVALID_RESPONSE,
                            RewindStatusErrorType.AUTHORIZATION_REQUIRED), it.message)
                    val payload = FetchedRewindStatePayload(error, site)
                    mDispatcher.dispatch(ActivityActionBuilder.newFetchedRewindStateAction(payload))
                })
        mRestClient.add(request)
    }

    private fun buildActivityPayload(activityResponses: List<ActivitiesResponse.ActivityResponse>,
                                     site: SiteModel,
                                     number: Int,
                                     offset: Int): FetchedActivitiesPayload {
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
                else -> {
                    val activityModelBuilder = ActivityLogModel.Builder(
                            localSiteId = site.id,
                            remoteSiteId = site.siteId,
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
                            discarded = it.is_discarded,
                            displayName = it.actor?.name,
                            actorType = it.actor?.type,
                            wpcomUserID = it.actor?.wpcom_user_id,
                            avatarURL = it.actor?.icon?.url,
                            role = it.actor?.role)
                    activityModelBuilder.build()
                }
            }
        }
        error?.let {
            return FetchedActivitiesPayload(ActivityError(it), site, number, offset)
        }
        return FetchedActivitiesPayload(activities, site, number, offset)
    }

    private fun buildRewindStatusPayload(response: RewindStatusResponse, site: SiteModel):
            FetchedRewindStatePayload {
        if (response.state == null) {
            return error(site, RewindStatusErrorType.MISSING_STATE)
        }
        if (RewindStatusModel.State.fromValue(response.state) == null) {
            return error(site, RewindStatusErrorType.INVALID_REWIND_STATE)
        }
        if (response.restoreResponse != null && response.restoreResponse.rewind_id == null) {
            return error(site, RewindStatusErrorType.MISSING_RESTORE_ID)
        }
        if (response.restoreResponse != null && response.restoreResponse.status == null) {
            return error(site, RewindStatusErrorType.MISSING_RESTORE_STATUS)
        }
        if (response.restoreResponse?.status != null
                && RewindStatusModel.RestoreStatus.Status.fromValue(response.restoreResponse.status) == null) {
            return error(site, RewindStatusErrorType.INVALID_RESTORE_STATUS)
        }
        val rewindStatusBuilder = RewindStatusModel.Builder(localSiteId = site.id,
                remoteSiteId = site.siteId,
                rewindState = response.state,
                reason = response.reason,
                restoreId = response.restoreResponse?.rewind_id,
                restoreErrorCode = response.restoreResponse?.error_code,
                restoreState = response.restoreResponse?.status,
                restoreProgress = response.restoreResponse?.progress,
                restoreMessage = response.restoreResponse?.message,
                restoreFailureReason = response.restoreResponse?.reason)
        return FetchedRewindStatePayload(rewindStatusBuilder.build(), site)
    }

    private fun error(site: SiteModel, errorType: RewindStatusErrorType) =
            FetchedRewindStatePayload(RewindStatusError(errorType), site)

    private fun <T> genericToError(error: BaseRequest.BaseNetworkError,
                                   genericError: T,
                                   invalidResponse: T,
                                   authorizationRequired: T): T {
        var errorType = genericError
        if (error.isGeneric && error.type == BaseRequest.GenericErrorType.INVALID_RESPONSE) {
            errorType = invalidResponse
        }
        if (error is WPComGsonRequest.WPComGsonNetworkError) {
            if ("unauthorized" == error.apiError) {
                errorType = authorizationRequired
            }
        }
        return errorType
    }

    data class ActivitiesResponse(val totalItems: Int?,
                                  val summary: String?,
                                  val current: Page) {
        data class Page(val orderedItems: List<ActivityResponse>)
        data class ActivityResponse(val summary: String?,
                                    val content: Content?,
                                    val name: String?,
                                    val actor: Actor?,
                                    val type: String?,
                                    val published: Date?,
                                    val generator: Generator?,
                                    val is_rewindable: Boolean?,
                                    val rewind_id: Float?,
                                    val gridicon: String?,
                                    val status: String?,
                                    val activity_id: String?,
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
