package org.wordpress.android.fluxc.network.rest.wpcom.vertical

import android.content.Context
import com.android.volley.RequestQueue
import kotlinx.coroutines.experimental.delay
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.vertical.SegmentPromptModel
import org.wordpress.android.fluxc.model.vertical.VerticalModel
import org.wordpress.android.fluxc.model.vertical.VerticalSegmentModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.VerticalStore.FetchedSegmentPromptPayload
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
                    iconColor = "#0000FF",
                    segmentId = it
            )
        }
        return FetchedSegmentsPayload(segmentList)
    }

    suspend fun fetchSegmentPrompt(segmentId: Long): FetchedSegmentPromptPayload {
        // TODO: Implement the actual call
        delay(1000)
        val prompt = SegmentPromptModel(
                title = "Title-$segmentId",
                subtitle = "Subtitle-$segmentId",
                hint = "Hint-$segmentId"
        )
        return FetchedSegmentPromptPayload(prompt)
    }

    suspend fun fetchVerticals(searchQuery: String, limit: Int): FetchedVerticalsPayload {
        // TODO: Implement the actual call
        delay(1000)
        val verticalList = (1..limit).map {
            VerticalModel(
                    name = "name-$it",
                    verticalId = "vertical-id-$it",
                    isNewUserVertical = it == limit
            )
        }
        return FetchedVerticalsPayload(verticalList)
    }
}
