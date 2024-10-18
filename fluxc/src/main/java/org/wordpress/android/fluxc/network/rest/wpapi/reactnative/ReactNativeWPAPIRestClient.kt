package org.wordpress.android.fluxc.network.rest.wpapi.reactnative

import com.android.volley.RequestQueue
import com.google.gson.JsonElement
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.BaseWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ReactNativeWPAPIRestClient @Inject constructor(
    private val wpApiGsonRequestBuilder: WPAPIGsonRequestBuilder,
    dispatcher: Dispatcher,
    @Named("custom-ssl") requestQueue: RequestQueue,
    userAgent: UserAgent
) : BaseWPAPIRestClient(dispatcher, requestQueue, userAgent) {
    suspend fun getRequest(
        url: String,
        params: Map<String, String>,
        successHandler: (data: JsonElement?) -> ReactNativeFetchResponse,
        errorHandler: (BaseNetworkError) -> ReactNativeFetchResponse,
        nonce: String? = null,
        enableCaching: Boolean = true
    ): ReactNativeFetchResponse {
        val response =
                wpApiGsonRequestBuilder.syncGetRequest(
                        this,
                        url,
                        params,
                        emptyMap(),
                        JsonElement::class.java,
                        enableCaching,
                        nonce = nonce)
        return when (response) {
            is Success -> successHandler(response.data)
            is Error -> errorHandler(response.error)
        }
    }

    suspend fun postRequest(
        url: String,
        body: Map<String, Any>,
        successHandler: (data: JsonElement?) -> ReactNativeFetchResponse,
        errorHandler: (BaseNetworkError) -> ReactNativeFetchResponse,
        nonce: String? = null,
    ): ReactNativeFetchResponse {
        val response =
            wpApiGsonRequestBuilder.syncPostRequest(
                this,
                url,
                body,
                JsonElement::class.java,
                nonce = nonce)
        return when (response) {
            is Success -> successHandler(response.data)
            is Error -> errorHandler(response.error)
        }
    }
}
