package org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn

import android.content.Context
import com.android.volley.RequestQueue
import com.android.volley.Response
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PasskeyRestClient @Inject constructor(
    context: Context,
    dispatcher: Dispatcher,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(
    context,
    dispatcher,
    requestQueue,
    accessToken,
    userAgent
) {
    @Suppress("LongParameterList")
    fun authenticateWebauthnSignature(
        userId: String,
        twoStepNonce: String,
        clientData: String,
        clientId: String,
        secret: String,
        onSuccess: (response: String) -> Unit,
        onFailure: (error: WPComGsonNetworkError) -> Unit
    ) {
        val parameters = mapOf(
            "user_id" to userId,
            "two_step_nonce" to twoStepNonce,
            "auth_type" to "webauthn",
            "client_data" to clientData,
            "client_id" to clientId,
            "client_secret" to secret,
            "get_bearer_token" to "true",
            "create_2fa_cookies_only" to "true"
        )

        triggerAccountRequest(
            url = webauthnAuthEndpointUrl,
            parameters = parameters,
            onSuccess = { onSuccess(it.asBearerToken) },
            onFailure = onFailure
        )
    }

    private fun triggerAccountRequest(
        url: String,
        parameters: Map<String, String>,
        onSuccess: (response: Map<*, *>) -> Unit,
        onFailure: (error: WPComGsonNetworkError) -> Unit
    ) {
        val successListener = Response.Listener<Map<*, *>> { onSuccess(it) }
        val failureListener = WPComErrorListener { onFailure(it) }

        val request = WPComGsonRequest.buildPostRequest(
            url,
            parameters,
            Map::class.java,
            successListener,
            failureListener
        )

        add(request)
    }

    private val Map<*, *>.asBearerToken: String
        get() = this["data"]
            ?.run { this as? Map<*, *> }
            ?.let { this["bearer_token"] as? String }
            .orEmpty()

    companion object {
        private const val baseWPLoginUrl = "https://wordpress.com/wp-login.php?action"
        private const val challengeEndpoint = "webauthn-challenge-endpoint"
        private const val authEndpoint = "webauthn-authentication-endpoint"
        const val webauthnChallengeEndpointUrl = "$baseWPLoginUrl=$challengeEndpoint"
        const val webauthnAuthEndpointUrl = "$baseWPLoginUrl=$authEndpoint"
    }
}
