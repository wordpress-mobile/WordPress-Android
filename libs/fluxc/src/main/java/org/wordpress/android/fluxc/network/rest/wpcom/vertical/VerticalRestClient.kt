package org.wordpress.android.fluxc.network.rest.wpcom.vertical

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.vertical.VerticalModel
import org.wordpress.android.fluxc.model.vertical.VerticalSegmentModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.VerticalStore.FetchSegmentsError
import org.wordpress.android.fluxc.store.VerticalStore.FetchVerticalsError
import org.wordpress.android.fluxc.store.VerticalStore.FetchedSegmentsPayload
import org.wordpress.android.fluxc.store.VerticalStore.FetchedVerticalsPayload
import org.wordpress.android.fluxc.store.VerticalStore.VerticalErrorType.GENERIC_ERROR
import javax.inject.Singleton

private const val PARAM_VERTICAL_SEARCH_QUERY = "search"
private const val PARAM_VERTICAL_SEARCH_LIMIT = "limit"

private class FetchSegmentsResponse : ArrayList<VerticalSegmentModel>()
private class FetchVerticalsResponse : ArrayList<VerticalModel>()

@Singleton
class VerticalRestClient
constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchSegments(): FetchedSegmentsPayload {
        val url = WPCOMV2.segments.url
        val response = wpComGsonRequestBuilder.syncGetRequest(this, url, emptyMap(), FetchSegmentsResponse::class.java)
        return when (response) {
            is Success -> FetchedSegmentsPayload(response.data)
            is Error -> FetchedSegmentsPayload(FetchSegmentsError(GENERIC_ERROR))
        }
    }

    suspend fun fetchVerticals(searchQuery: String, limit: Int): FetchedVerticalsPayload {
        val url = WPCOMV2.verticals.url
        val params = HashMap<String, String>()
        params[PARAM_VERTICAL_SEARCH_QUERY] = searchQuery
        params[PARAM_VERTICAL_SEARCH_LIMIT] = limit.toString()
        val response = wpComGsonRequestBuilder.syncGetRequest(this, url, params, FetchVerticalsResponse::class.java)
        return when (response) {
            is Success -> FetchedVerticalsPayload(response.data)
            is Error -> FetchedVerticalsPayload(FetchVerticalsError(GENERIC_ERROR))
        }
    }
}
