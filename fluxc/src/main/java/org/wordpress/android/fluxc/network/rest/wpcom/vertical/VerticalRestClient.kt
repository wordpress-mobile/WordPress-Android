package org.wordpress.android.fluxc.network.rest.wpcom.vertical

import android.content.Context
import com.android.volley.RequestQueue
import kotlinx.coroutines.experimental.delay
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.vertical.VerticalModel
import org.wordpress.android.fluxc.model.vertical.VerticalSegmentModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.VerticalStore.FetchedSegmentsPayload
import org.wordpress.android.fluxc.store.VerticalStore.FetchedVerticalsPayload
import javax.inject.Singleton

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
        // TODO: Implement the actual call
        delay(1000)
        val segmentList = (1..100L).map {
            VerticalSegmentModel(
                    title = "title-$it",
                    subtitle = "subtitle-$it",
                    iconUrl = "https://picsum.photos/50",
                    segmentId = it
            )
        }
        return FetchedSegmentsPayload(segmentList)
    }

    suspend fun fetchVerticals(searchQuery: String): FetchedVerticalsPayload {
        // TODO: Implement the actual call
        delay(1000)
        val verticalList = (1..100).map { VerticalModel(name = "name-$it", verticalId = "vertical-id-$it") }
        return FetchedVerticalsPayload(verticalList)
    }
}
