package org.wordpress.android.fluxc.network.rest.wpcom

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * This class acts as a network engine for using Jetpack Tunnel to call WP API endpoints.
 * The goal of adding this class instead of directly inheriting from [BaseWPComRestClient] is allowing to move away
 * from the traditional model of inheritance, and allowing the feature RestClients to have multiple network
 * implementations at the same time when it's needed
 */
@Singleton
class WPComNetwork @Inject constructor(
    appContext: Context,
    dispatcher: Dispatcher,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun <T : Any> executeGetGsonRequest(
        url: String,
        clazz: Class<T>,
        params: Map<String, String> = emptyMap(),
        enableCaching: Boolean = false,
        cacheTimeToLive: Int = BaseRequest.DEFAULT_CACHE_LIFETIME,
        forced: Boolean = false
    ): WPComGsonRequestBuilder.Response<T> {
        return wpComGsonRequestBuilder.syncGetRequest(
            restClient = this,
            url = url,
            params = params,
            clazz = clazz,
            enableCaching = enableCaching,
            cacheTimeToLive = cacheTimeToLive,
            forced = forced
        )
    }

    suspend fun <T : Any> executePostGsonRequest(
        url: String,
        clazz: Class<T>,
        params: Map<String, String> = emptyMap(),
        body: Map<String, Any> = emptyMap(),
    ): WPComGsonRequestBuilder.Response<T> {
        return wpComGsonRequestBuilder.syncPostRequest(
            restClient = this,
            url = url,
            clazz = clazz,
            params = params,
            body = body
        )
    }
}
