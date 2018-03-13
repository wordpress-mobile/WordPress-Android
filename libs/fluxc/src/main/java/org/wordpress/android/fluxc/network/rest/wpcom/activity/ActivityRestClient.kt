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
        val request = WPComGsonRequest.buildGetRequest(
                url, params, ActivityResponse::class.java,
                {
                    Log.d("activity_log", "Response: $it")
                    val activities = it.current.orderedItems
                    val payload = ActivityStore.FetchActivitiesResponsePayload(activities, site, number, offset)
                    mDispatcher.dispatch(ActivityActionBuilder.newFetchedActivitiesAction(payload))
                },
                {
                    val error = ActivityStore.ActivityError(genericToError(it), it.message)
                    val payload = ActivityStore.FetchActivitiesResponsePayload(error, site, number, offset)
                    mDispatcher.dispatch(ActivityActionBuilder.newFetchedActivitiesAction(payload))
                })
        add(request)
    }

    private fun genericToError(error: BaseRequest.BaseNetworkError): ActivityStore.ActivityErrorType {
        var errorType = ActivityStore.ActivityErrorType.GENERIC_ERROR
        if (error.isGeneric && error.type == BaseRequest.GenericErrorType.INVALID_RESPONSE) {
            errorType = ActivityStore.ActivityErrorType.INVALID_RESPONSE
        }
        if (error is WPComGsonRequest.WPComGsonNetworkError) {
            if ("unauthorized" == error.apiError) {
                errorType = ActivityStore.ActivityErrorType.AUTHORIZATION_REQUIRED
            }
        }
        return errorType
    }
}

data class ActivityResponse(val totalItems: Int?,
                            val summary: String?,
                            val current: Page) {
    data class Page(val orderedItems: List<Activity>)
    data class Activity(val summary: String?,
                        val content: Content?,
                        val name: String?,
                        val actor: Actor,
                        val type: String?,
                        val published: Date?,
                        val generator: Generator)

    data class Content(val text: String?)
    data class Actor(val type: String?,
                     val name: String?,
                     val external_user_id: Long?,
                     val wpcom_user_id: Long?,
                     val icon: Icon?,
                     val role: String?)

    data class Icon(val type: String?, val url: String?, val width: Int?, val height: Int?)
    data class Generator(val jetpack_version: Int?,
                         val blog_id: Long?,
                         val is_rewindable: Boolean?,
                         val rewind_id: Float?,
                         val gridicon: String?,
                         val status: String?,
                         val activity_id: String,
                         val is_discarded: Boolean?)
}
