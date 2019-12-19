package org.wordpress.android.fluxc.network.rest.wpapi.reactnative

import com.android.volley.RequestQueue
import com.google.gson.JsonElement
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.BaseWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder.Response.Error as Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ReactNativeWPAPIRestClient @Inject constructor(
    private val wpapiGsonRequestBuilder: WPAPIGsonRequestBuilder,
    dispatcher: Dispatcher,
    @Named("regular") requestQueue: RequestQueue,
    userAgent: UserAgent
) : BaseWPAPIRestClient(dispatcher, requestQueue, userAgent) {
    suspend fun fetch(
        url: String,
        params: Map<String, String>,
        successHandler: (data: JsonElement) -> ReactNativeFetchResponse,
        errorHandler: (BaseNetworkError) -> ReactNativeFetchResponse
    ): ReactNativeFetchResponse {
        val response =
                wpapiGsonRequestBuilder.syncGetRequest(this, url, params, emptyMap(), JsonElement::class.java, true)
        return when (response) {
            is Success -> successHandler(response.data)
            is Error -> errorHandler(response.error)
        }
    }
}
