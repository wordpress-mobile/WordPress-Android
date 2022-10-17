package org.wordpress.android.fluxc.network.rest.wpcom.account

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import javax.inject.Inject
import javax.inject.Named

class SignUpRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {

    suspend fun fetchUsernameSuggestions(username: String): SignUpWPAPIPayload<List<String>> {
        val url = WPCOMV2.users.username.suggestions.url
        val response = wpComGsonRequestBuilder.syncGetRequest(
            restClient = this,
            url = url,
            params = mapOf("name" to username),
            UsernameSuggestionsResponse::class.java
        )
        return when (response) {
            is WPComGsonRequestBuilder.Response.Success -> SignUpWPAPIPayload(response.data.suggestions)
            is WPComGsonRequestBuilder.Response.Error -> SignUpWPAPIPayload(response.error)
        }
    }

    data class SignUpWPAPIPayload<T>(
        val result: T?
    ) : Payload<BaseNetworkError?>() {
        constructor(error: BaseNetworkError) : this(null) {
            this.error = error
        }
    }
}