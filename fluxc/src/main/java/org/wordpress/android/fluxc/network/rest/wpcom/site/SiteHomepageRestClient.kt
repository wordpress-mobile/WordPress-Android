package org.wordpress.android.fluxc.network.rest.wpcom.site

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.SiteHomepageSettings
import org.wordpress.android.fluxc.model.SiteHomepageSettings.StaticPage
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront
import org.wordpress.android.fluxc.model.SiteHomepageSettingsMapper
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.SiteOptionsStore.HomepageUpdatedPayload
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SiteHomepageRestClient
@Inject constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    private val siteHomepageSettingsMapper: SiteHomepageSettingsMapper,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun updateHomepage(
        site: SiteModel,
        homepageSettings: SiteHomepageSettings
    ): HomepageUpdatedPayload {
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
        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                params,
                UpdateHomepageResponse::class.java
        )
        return when (response) {
            is Success -> {
                val updatedHomepageSettings = siteHomepageSettingsMapper.map(response.data)
                if (updatedHomepageSettings is StaticPage) {
                    site.pageForPosts = updatedHomepageSettings.pageForPostsId
                    site.pageOnFront = updatedHomepageSettings.pageOnFrontId
                }
                site.showOnFront = updatedHomepageSettings.showOnFront.value
                mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(site))
                HomepageUpdatedPayload(updatedHomepageSettings)
            }
            is Error -> {
                HomepageUpdatedPayload(response.error)
            }
        }
    }

    data class UpdateHomepageResponse(
        @SerializedName("is_page_on_front") val isPageOnFront: Boolean,
        @SerializedName("page_on_front_id") val pageOnFrontId: Long?,
        @SerializedName("page_for_posts_id") val pageForPostsId: Long?
    )
}
