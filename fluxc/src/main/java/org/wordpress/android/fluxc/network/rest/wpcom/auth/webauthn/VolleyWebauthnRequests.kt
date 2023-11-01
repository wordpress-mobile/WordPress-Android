package org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn

import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.Response.ErrorListener
import com.android.volley.toolbox.HttpHeaderParser
import org.json.JSONObject
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnRequestParameters.AUTH_TYPE
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnRequestParameters.CLIENT_DATA
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnRequestParameters.CLIENT_ID
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnRequestParameters.CLIENT_SECRET
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnRequestParameters.CREATE_2FA_COOKIES_ONLY
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnRequestParameters.GET_BEARER_TOKEN
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnRequestParameters.TWO_STEP_NONCE
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnRequestParameters.USER_ID

private const val WEBAUTHN_AUTH_TYPE = "webauthn"
private const val baseWPLoginUrl = "https://wordpress.com/wp-login.php?action"
private const val challengeEndpoint = "webauthn-challenge-endpoint"
private const val authEndpoint = "webauthn-authentication-endpoint"
private const val webauthnChallengeEndpointUrl = "$baseWPLoginUrl=$challengeEndpoint"
private const val webauthnAuthEndpointUrl = "$baseWPLoginUrl=$authEndpoint"

private enum class WebauthnRequestParameters(val value: String) {
    USER_ID("user_id"),
    AUTH_TYPE("auth_type"),
    TWO_STEP_NONCE("two_step_nonce"),
    CLIENT_ID("client_id"),
    CLIENT_SECRET("client_secret"),
    CLIENT_DATA("client_data"),
    GET_BEARER_TOKEN("get_bearer_token"),
    CREATE_2FA_COOKIES_ONLY("create_2fa_cookies_only")
}

class WebauthnChallengeRequestException(message: String): Exception(message)

private fun NetworkResponse?.extractResult(parameterName: String): Response<String> {
    if (this == null) {
        val error = WebauthnChallengeRequestException("Webauthn challenge response is null")
        return Response.error(ParseError(error))
    }

    return try {
        val headers = HttpHeaderParser.parseCacheHeaders(this)
        val charsetName = HttpHeaderParser.parseCharset(this.headers)
        return String(this.data, charset(charsetName))
            .let { JSONObject(it).getJSONObject(parameterName) }
            .let { Response.success(it.toString(), headers) }
    } catch (exception: Exception) {
        val error = WebauthnChallengeRequestException("Webauthn challenge response is invalid")
        Response.error(ParseError(error))
    }
}

class WebauthnChallengeRequest(
    userId: String,
    twoStepNonce: String,
    clientId: String,
    clientSecret: String,
    errorListener: ErrorListener,
    private val listener: Response.Listener<String>
): Request<String>(Method.POST, webauthnChallengeEndpointUrl, errorListener) {
    private val parameters: Map<String, String> = mapOf(
        CLIENT_ID.value to clientId,
        CLIENT_SECRET.value to clientSecret,
        USER_ID.value to userId,
        AUTH_TYPE.value to WEBAUTHN_AUTH_TYPE,
        TWO_STEP_NONCE.value to twoStepNonce
    )

    override fun getParams() = parameters
    override fun deliverResponse(response: String) = listener.onResponse(response)
    override fun parseNetworkResponse(response: NetworkResponse?) =
        response.extractResult(responseParameterName)

    companion object {
        private const val responseParameterName = "data"
    }
}

class WebauthnTokenRequest(
    userId: String,
    twoStepNonce: String,
    clientId: String,
    clientSecret: String,
    clientData: String,
    errorListener: ErrorListener,
    private val listener: Response.Listener<String>
) : Request<String>(Method.POST, webauthnAuthEndpointUrl, errorListener) {
    private val parameters = mapOf(
        CLIENT_ID.value to clientId,
        CLIENT_SECRET.value to clientSecret,
        USER_ID.value to userId,
        AUTH_TYPE.value to WEBAUTHN_AUTH_TYPE,
        TWO_STEP_NONCE.value to twoStepNonce,
        CLIENT_DATA.value to clientData,
        GET_BEARER_TOKEN.value to "true",
        CREATE_2FA_COOKIES_ONLY.value to "true"
    )

    override fun parseNetworkResponse(response: NetworkResponse?) =
        response.extractResult(responseParameterName)

    override fun getParams() = parameters
    override fun deliverResponse(response: String) = listener.onResponse(response)

    companion object {
        private const val responseParameterName = "bearer_token"
    }
}