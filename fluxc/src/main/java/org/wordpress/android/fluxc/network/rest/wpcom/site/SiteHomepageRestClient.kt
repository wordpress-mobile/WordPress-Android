package org.wordpress.android.fluxc.network.rest.wpcom.site

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.SiteOptionsStore.UpdateHomepagePayload
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.toStatsError
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SiteHomepageRestClient
@Inject constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun updateHomepage(
        site: SiteModel,
        isPageOnFront: Boolean,
        pageOnFrontId: Int,
        pageForPostsId: Int
    ): UpdateHomepagePayload {
        val url = WPCOMREST.sites.site(site.siteId).homepage.urlV1_1
        val params = mapOf(
                "is_page_on_front" to isPageOnFront.toString(),
                "page_on_front_id" to pageOnFrontId.toString(),
                "page_for_posts_id" to pageForPostsId.toString()
        )
        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                params,
                UpdateHomepageResponse::class.java
        )
        return when (response) {
            is Success -> {
                UpdateHomepagePayload(response.data)
            }
            is Error -> {
                UpdateHomepagePayload(response.error)
            }
        }
    }

    data class UpdateHomepageResponse(
        @SerializedName("is_page_on_front") val isPageOnFront: Boolean,
        @SerializedName("page_on_front_id") val pageOnFrontId: Int,
        @SerializedName("page_for_posts_id") val pageForPostsId: Int
    )
}
