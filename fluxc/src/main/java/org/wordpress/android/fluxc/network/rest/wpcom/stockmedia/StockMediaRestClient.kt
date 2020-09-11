package org.wordpress.android.fluxc.network.rest.wpcom.stockmedia

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.StockMediaStore.FetchedStockMediaListPayload
import org.wordpress.android.fluxc.store.StockMediaStore.StockMediaError
import org.wordpress.android.fluxc.store.StockMediaStore.StockMediaErrorType
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MEDIA
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

@Singleton
class StockMediaRestClient
@Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    dispatcher: Dispatcher,
    appContext: Context?,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    /**
     * Gets a list of stock media items matching a query string
     */
    suspend fun searchStockMedia(searchTerm: String, page: Int, pageSize: Int): FetchedStockMediaListPayload {
        val url = WPCOMREST.meta.external_media.pexels.urlV1_1
        val params = mapOf(
                "number" to pageSize.toString(),
                "page_handle" to page.toString(),
                "source" to "pexels",
                "search" to UrlUtils.urlEncode(searchTerm)
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                SearchStockMediaResponse::class.java
        )
        return when (response) {
            is Success -> {
                val data = response.data
                FetchedStockMediaListPayload(
                        data.media,
                        searchTerm,
                        data.nextPage,
                        data.canLoadMore
                )
            }
            is Error -> {
                val error = response.error
                AppLog.e(MEDIA, "VolleyError Fetching stock media: $error")
                val mediaError = StockMediaError(
                        StockMediaErrorType.fromBaseNetworkError(error), error.message
                )
                FetchedStockMediaListPayload(mediaError, searchTerm)
            }
        }
    }
}
