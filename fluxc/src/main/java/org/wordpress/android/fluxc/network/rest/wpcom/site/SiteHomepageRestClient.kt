package org.wordpress.android.fluxc.network.rest.wpcom.site

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.SiteHomepageSettings
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront
import org.wordpress.android.fluxc.model.SiteHomepageSettings.StaticPage
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
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
        homepageSettings: SiteHomepageSettings
    ): Response<UpdateHomepageResponse> {
        val url = WPCOMREST.sites.site(site.siteId).homepage.urlV1_1
        val params = mutableMapOf(
                "is_page_on_front" to (homepageSettings.showOnFront == ShowOnFront.PAGE).toString()
        )
        if (homepageSettings is StaticPage) {
            if (homepageSettings.pageOnFrontId > -1) {
                params["page_on_front_id"] = homepageSettings.pageOnFrontId.toString()
            }
            if (homepageSettings.pageForPostsId > -1) {
                params["page_for_posts_id"] = homepageSettings.pageForPostsId.toString()
            }
        }
        return wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                params,
                UpdateHomepageResponse::class.java
        )
    }

    data class UpdateHomepageResponse(
        @SerializedName("is_page_on_front") val isPageOnFront: Boolean,
        @SerializedName("page_on_front_id") val pageOnFrontId: Long?,
        @SerializedName("page_for_posts_id") val pageForPostsId: Long?
    )
}
