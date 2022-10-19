package org.wordpress.android.fluxc.network.rest.wpcom.account.signup

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.account.UsernameSuggestionsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets
import javax.inject.Inject
import javax.inject.Named

@SuppressWarnings("LongParameterList")
class SignUpRestClient @Inject constructor(
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    private val appSecrets: AppSecrets
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
            is Success -> SignUpWPAPIPayload(response.data.suggestions)
            is Error -> SignUpWPAPIPayload(response.error)
        }
    }

    suspend fun createWPAccount(
        email: String,
        password: String,
        username: String
    ): SignUpWPAPIPayload<AccountCreatedDto> {
        val url = WPCOMREST.users.new_.urlV1_1
        val body = mapOf(
            "email" to email,
            "password" to password,
            "username" to username,
            "validate" to true,
            "client_id" to appSecrets.appId,
            "client_secret" to appSecrets.appSecret,
            "signup_flow_name" to "mobile-android",
            "flow" to "signup",
            "send_verification_email" to true
        )
        val response = wpComGsonRequestBuilder.syncPostRequest(
            restClient = this,
            url = url,
            params = null,
            body = body,
            clazz = AccountCreatedDto::class.java
        )
        return when (response) {
            is Success -> SignUpWPAPIPayload(response.data)
            is Error -> SignUpWPAPIPayload(response.error)
        }
    }

    data class SignUpWPAPIPayload<T>(
        val result: T?
    ) : Payload<WPComGsonNetworkError?>() {
        constructor(error: WPComGsonNetworkError) : this(null) {
            this.error = error
        }
    }
}
