package org.wordpress.android.fluxc.network.rest.wpcom.theme

import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.BaseWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ThemeCoroutineRestClient @Inject constructor(
    private val wpApiGsonRequestBuilder: WPAPIGsonRequestBuilder,
    dispatcher: Dispatcher,
    @Named("custom-ssl") requestQueue: RequestQueue,
    userAgent: UserAgent
) : BaseWPAPIRestClient(dispatcher, requestQueue, userAgent) {
    suspend fun fetchThemeDemoPages(
        themeDemoUrl: String
    ): ThemeDemoDataWPAPIPayload<Array<DemoPageResponse>> {
        val url = themeDemoUrl + WP_DEMO_THEME_PAGES_URL
        val response = wpApiGsonRequestBuilder.syncGetRequest(
            restClient = this,
            url = url,
            clazz = Array<DemoPageResponse>::class.java,
            params = mapOf("per_page" to MAX_NUMBER_OF_DEMO_PAGES.toString()),
        )
        return when (response) {
            is WPAPIResponse.Success -> ThemeDemoDataWPAPIPayload(response.data)
            is WPAPIResponse.Error -> ThemeDemoDataWPAPIPayload(response.error)
        }
    }

    data class ThemeDemoDataWPAPIPayload<T>(
        val result: T?
    ) : Payload<BaseNetworkError?>() {
        constructor(error: BaseNetworkError) : this(null) {
            this.error = error
        }
    }

    companion object {
        private const val MAX_NUMBER_OF_DEMO_PAGES = 30
        private const val WP_DEMO_THEME_PAGES_URL = "/wp-json/wp/v2/pages"
    }
}
