package org.wordpress.android.fluxc.network.rest.wpcom.stockmedia

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.StockMediaActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.StockMediaStore.FetchedStockMediaListPayload
import org.wordpress.android.fluxc.store.StockMediaStore.StockMediaError
import org.wordpress.android.fluxc.store.StockMediaStore.StockMediaErrorType.Companion.fromBaseNetworkError
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MEDIA
import org.wordpress.android.util.UrlUtils
import java.util.HashMap
import javax.inject.Singleton

@Singleton
class StockMediaRestClient(
    appContext: Context?,
    dispatcher: Dispatcher?,
    requestQueue: RequestQueue?,
    accessToken: AccessToken?,
    userAgent: UserAgent?
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    /**
     * Gets a list of stock media items matching a query string
     */
    fun searchStockMedia(searchTerm: String, page: Int) {
        val url = WPCOMREST.meta.external_media.pexels.urlV1_1
        val params = mapOf(
                "number" to DEFAULT_NUM_STOCK_MEDIA_PER_FETCH.toString(),
                "source" to "pexels",
                "page_handle" to page.toString(),
                "search" to UrlUtils.urlEncode(searchTerm)
        )
        val request: WPComGsonRequest<*> = WPComGsonRequest.buildGetRequest(url,
                params,
                SearchStockMediaResponse::class.java,
                { response ->
                    val payload = FetchedStockMediaListPayload(
                            response.media,
                            searchTerm,
                            response.nextPage,
                            response.canLoadMore
                    )
                    mDispatcher.dispatch(StockMediaActionBuilder.newFetchedStockMediaAction(payload))
                }) { error ->
            AppLog.e(MEDIA, "VolleyError Fetching stock media: $error")
            val mediaError = StockMediaError(
                    fromBaseNetworkError(error), error.message
            )
            val payload = FetchedStockMediaListPayload(mediaError, searchTerm)
            mDispatcher.dispatch(StockMediaActionBuilder.newFetchedStockMediaAction(payload))
        }
        add(request)
    }

    companion object {
        // this should be a multiple of both 3 and 4 since WPAndroid shows either 3 or 4 pics per row
        const val DEFAULT_NUM_STOCK_MEDIA_PER_FETCH = 36
    }
}
