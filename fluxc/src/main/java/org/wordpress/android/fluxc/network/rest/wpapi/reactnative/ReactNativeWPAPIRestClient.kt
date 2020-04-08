package org.wordpress.android.fluxc.network.rest.wpapi.reactnative

import androidx.annotation.VisibleForTesting
import com.android.volley.NoConnectionError
import com.android.volley.RequestQueue
import com.google.gson.JsonElement
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.BaseWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIEncodedBodyRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error as Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.network.rest.wpapi.reactnative.Nonce.Available
import org.wordpress.android.fluxc.network.rest.wpapi.reactnative.Nonce.Unknown
import org.wordpress.android.fluxc.network.rest.wpapi.reactnative.Nonce.FailedRequest
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse
import org.wordpress.android.fluxc.store.ReactNativeStore.Companion.slashJoin
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ReactNativeWPAPIRestClient @VisibleForTesting constructor(
    private val wpApiGsonRequestBuilder: WPAPIGsonRequestBuilder,
    private val wpApiEncodedBodyRequestBuilder: WPAPIEncodedBodyRequestBuilder,
    dispatcher: Dispatcher,
    requestQueue: RequestQueue,
    userAgent: UserAgent,
    private val currentTimeMillis: () -> Long
) : BaseWPAPIRestClient(dispatcher, requestQueue, userAgent) {
    @Inject constructor(
        wpApiGsonRequestBuilder: WPAPIGsonRequestBuilder,
        wpApiEncodedBodyRequestBuilder: WPAPIEncodedBodyRequestBuilder,
        dispatcher: Dispatcher,
        @Named("custom-ssl") requestQueue: RequestQueue,
        userAgent: UserAgent
    ) : this(
            wpApiGsonRequestBuilder,
            wpApiEncodedBodyRequestBuilder,
            dispatcher, requestQueue,
            userAgent,
            System::currentTimeMillis
    )

    suspend fun fetch(
        url: String,
        params: Map<String, String>,
        successHandler: (data: JsonElement) -> ReactNativeFetchResponse,
        errorHandler: (BaseNetworkError) -> ReactNativeFetchResponse,
        nonce: String? = null
    ): ReactNativeFetchResponse {
        val response =
                wpApiGsonRequestBuilder.syncGetRequest(
                        this,
                        url,
                        params,
                        emptyMap(),
                        JsonElement::class.java,
                        true,
                        nonce = nonce)
        return when (response) {
            is Success -> successHandler(response.data)
            is Error -> errorHandler(response.error)
        }
    }

    /**
     *  Requests a nonce using the
     *  [rest-nonce endpoint](https://developer.wordpress.org/reference/functions/wp_ajax_rest_nonce/)
     *  that became available in WordPress 5.3.
     */
    suspend fun requestNonce(site: SiteModel): Nonce {
        val wpLoginUrl = slashJoin(site.url, "wp-login.php")
        val redirectUrl = slashJoin(site.url, "wp-admin/admin-ajax.php?action=rest-nonce")
        val body = mapOf(
                "log" to site.username,
                "pwd" to site.password,
                "redirect_to" to redirectUrl
        )
        val response =
                wpApiEncodedBodyRequestBuilder.syncPostRequest(this, wpLoginUrl, body = body)
        return when (response) {
            is Success -> if (response.data.matches("[0-9a-zA-Z]{2,}".toRegex())) {
                Available(response.data)
            } else {
                FailedRequest(currentTimeMillis())
            }
            is Error -> {
                if (response.error.volleyError is NoConnectionError) {
                    // No connection, so we do not know if a nonce is available
                    Unknown
                } else {
                    FailedRequest(currentTimeMillis())
                }
            }
        }
    }
}
