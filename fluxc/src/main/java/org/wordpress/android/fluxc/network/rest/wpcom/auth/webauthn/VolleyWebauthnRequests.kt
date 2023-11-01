package org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn

import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.Response.ErrorListener
import com.android.volley.toolbox.HttpHeaderParser
import org.json.JSONException
import org.json.JSONObject
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnRequestParameters.AUTH_TYPE
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnRequestParameters.CLIENT_DATA
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnRequestParameters.CLIENT_ID
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnRequestParameters.CLIENT_SECRET
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnRequestParameters.CREATE_2FA_COOKIES_ONLY
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnRequestParameters.GET_BEARER_TOKEN
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnRequestParameters.TWO_STEP_NONCE
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnRequestParameters.USER_ID
import java.io.UnsupportedEncodingException

class WebauthnChallengeRequest(
    userId: String,
    twoStepNonce: String,
    clientId: String,
    clientSecret: String,
    listener: Response.Listener<String>,
    errorListener: ErrorListener
): BaseWebauthnRequest(webauthnChallengeEndpointUrl, errorListener, listener) {
    override val parameters: Map<String, String> = mapOf(
        CLIENT_ID.value to clientId,
        CLIENT_SECRET.value to clientSecret,
        USER_ID.value to userId,
        AUTH_TYPE.value to WEBAUTHN_AUTH_TYPE,
        TWO_STEP_NONCE.value to twoStepNonce
    )

    override val responseParameterName = "data"
}

@SuppressWarnings("LongParameterList")
class WebauthnTokenRequest(
    userId: String,
    twoStepNonce: String,
    clientId: String,
    clientSecret: String,
    clientData: String,
    listener: Response.Listener<String>,
    errorListener: ErrorListener
) : BaseWebauthnRequest(webauthnAuthEndpointUrl, errorListener, listener) {
    override val parameters = mapOf(
        CLIENT_ID.value to clientId,
        CLIENT_SECRET.value to clientSecret,
        USER_ID.value to userId,
        AUTH_TYPE.value to WEBAUTHN_AUTH_TYPE,
        TWO_STEP_NONCE.value to twoStepNonce,
        CLIENT_DATA.value to clientData,
        GET_BEARER_TOKEN.value to "true",
        CREATE_2FA_COOKIES_ONLY.value to "true"
    )

    override val responseParameterName = "bearer_token"
}

abstract class BaseWebauthnRequest(
    url: String,
    errorListener: ErrorListener,
    private val listener: Response.Listener<String>
) : Request<String>(Method.POST, url, errorListener) {
    abstract val parameters: Map<String, String>
    abstract val responseParameterName: String

    private fun NetworkResponse?.extractResult(parameterName: String): Response<String> {
        if (this == null) {
            val error = WebauthnChallengeRequestException("Webauthn challenge response is null")
            return Response.error(ParseError(error))
        }

        return try {
            val headers = HttpHeaderParser.parseCacheHeaders(this)
            val charsetName = HttpHeaderParser.parseCharset(this.headers)
            String(this.data, charset(charsetName))
                .let { JSONObject(it).getJSONObject(parameterName) }
                .let { Response.success(it.toString(), headers) }
        }
        catch (exception: UnsupportedEncodingException) { handleError(exception) }
        catch (exception: JSONException) { handleError(exception) }

    }

    private fun handleError(exception: Exception): Response<String> {
        val message = exception.message ?: "Webauthn challenge response is null"
        val error = WebauthnChallengeRequestException(message)
        return Response.error(ParseError(error))
    }

    override fun getParams() = parameters
    override fun deliverResponse(response: String) = listener.onResponse(response)
    override fun parseNetworkResponse(response: NetworkResponse?) =
        response.extractResult(responseParameterName)

    companion object {
        private const val baseWPLoginUrl = "https://wordpress.com/wp-login.php?action"
        private const val challengeEndpoint = "webauthn-challenge-endpoint"
        private const val authEndpoint = "webauthn-authentication-endpoint"

        internal const val webauthnChallengeEndpointUrl = "$baseWPLoginUrl=$challengeEndpoint"
        internal const val webauthnAuthEndpointUrl = "$baseWPLoginUrl=$authEndpoint"
        internal const val WEBAUTHN_AUTH_TYPE = "webauthn"
    }
}

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
