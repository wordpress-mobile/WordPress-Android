package org.wordpress.android.fluxc.network.rest.wpcom.vertical

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.vertical.VerticalSegmentModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.VerticalStore.FetchSegmentsError
import org.wordpress.android.fluxc.store.VerticalStore.FetchedSegmentsPayload
import org.wordpress.android.fluxc.store.VerticalStore.VerticalErrorType.GENERIC_ERROR
import javax.inject.Singleton

private class FetchSegmentsResponse : ArrayList<VerticalSegmentModel>()

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
}
