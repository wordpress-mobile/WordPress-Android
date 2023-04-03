package org.wordpress.android.fluxc.network.rest.wpcom.reactnative

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.JsonElement
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ReactNativeWPComRestClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    dispatcher: Dispatcher,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun getRequest(
        url: String,
        params: Map<String, String>,
        successHandler: (data: JsonElement) -> ReactNativeFetchResponse,
        errorHandler: (BaseNetworkError) -> ReactNativeFetchResponse,
        enableCaching: Boolean = true
    ): ReactNativeFetchResponse {
        val response =
                wpComGsonRequestBuilder.syncGetRequest(this, url, params, JsonElement::class.java, enableCaching)
        return when (response) {
            is Success -> successHandler(response.data)
            is Error -> errorHandler(response.error)
        }
    }

    suspend fun postRequest(
        url: String,
        params: Map<String, String>,
        body: Map<String, Any>,
        successHandler: (data: JsonElement) -> ReactNativeFetchResponse,
        errorHandler: (BaseNetworkError) -> ReactNativeFetchResponse
    ): ReactNativeFetchResponse {
        val response =
            wpComGsonRequestBuilder.syncPostRequest(this, url, params, body, JsonElement::class.java)
        return when (response) {
            is Success -> successHandler(response.data)
            is Error -> errorHandler(response.error)
        }
    }
}
