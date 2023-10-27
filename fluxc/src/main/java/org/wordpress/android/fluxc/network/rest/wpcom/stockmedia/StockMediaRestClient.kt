package org.wordpress.android.fluxc.network.rest.wpcom.stockmedia

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaResponseUtils
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaWPComRestResponse.MultipleMediaResponse
import org.wordpress.android.fluxc.store.MediaStore.UploadStockMediaError
import org.wordpress.android.fluxc.store.MediaStore.UploadStockMediaErrorType
import org.wordpress.android.fluxc.store.MediaStore.UploadedStockMediaPayload
import org.wordpress.android.fluxc.store.StockMediaStore.FetchedStockMediaListPayload
import org.wordpress.android.fluxc.store.StockMediaStore.StockMediaError
import org.wordpress.android.fluxc.store.StockMediaStore.StockMediaErrorType
import org.wordpress.android.fluxc.store.StockMediaUploadItem
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MEDIA
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.UrlUtils
import java.util.HashMap
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class StockMediaRestClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    private val mediaResponseUtils: MediaResponseUtils,
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
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
                val mediaError = StockMediaError(StockMediaErrorType.fromBaseNetworkError(), error.message)
                FetchedStockMediaListPayload(mediaError, searchTerm)
            }
        }
    }
    /**
     * Gets a list of stock media items matching a query string
     */

    suspend fun uploadStockMedia(
        site: SiteModel,
        stockMediaList: List<StockMediaUploadItem>
    ): UploadedStockMediaPayload {
        val url = WPCOMREST.sites.site(site.siteId).external_media_upload.urlV1_1
        val jsonBody = JsonArray()
        for (stockMedia in stockMediaList) {
            val json = JsonObject()
            json.addProperty("url", StringUtils.notNullStr(stockMedia.url))
            json.addProperty("name", StringUtils.notNullStr(stockMedia.name))
            json.addProperty("title", StringUtils.notNullStr(stockMedia.title))
            jsonBody.add(json.toString())
        }
        val body: MutableMap<String, Any> = HashMap()
        body["service"] = "pexels"
        body["external_ids"] = jsonBody

        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                null,
                body,
                MultipleMediaResponse::class.java
        )
        return when (response) {
            is Success -> {
                val mediaList: List<MediaModel> = mediaResponseUtils.getMediaListFromRestResponse(
                        response.data,
                        site.id
                )
                UploadedStockMediaPayload(site, mediaList)
            }
            is Error -> {
                val error = response.error
                AppLog.e(MEDIA, "VolleyError uploading stock media: $error")
                val mediaError = UploadStockMediaError(
                        UploadStockMediaErrorType.fromNetworkError(error), error.message
                )
                UploadedStockMediaPayload(site, mediaError)
            }
        }
    }
}
