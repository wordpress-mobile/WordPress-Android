package org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn

import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.Response.ErrorListener
import com.android.volley.toolbox.HttpHeaderParser
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException

abstract class BaseWebauthnRequest<T>(
    url: String,
    errorListener: ErrorListener,
    private val listener: Response.Listener<T>
) : Request<T>(Method.POST, url, errorListener) {
    abstract val parameters: Map<String, String>
    abstract fun serializeResponse(response: String): T

    internal val gson by lazy { Gson() }

    private fun NetworkResponse?.extractResult(): Response<T> {
        if (this == null) {
            val error = WebauthnChallengeRequestException("Webauthn challenge response is null")
            return Response.error(ParseError(error))
        }

        return try {
            val headers = HttpHeaderParser.parseCacheHeaders(this)
            val charsetName = HttpHeaderParser.parseCharset(this.headers)
            String(this.data, charset(charsetName))
                .let { JSONObject(it).getJSONObject(WEBAUTHN_DATA) }
                .let { serializeResponse(it.toString()) }
                .let { Response.success(it, headers) }
        }
        catch (exception: UnsupportedEncodingException) { Response.error(ParseError(exception)) }
        catch (exception: JSONException) { Response.error(ParseError(exception)) }
    }

    override fun getParams() = parameters
    override fun deliverResponse(response: T) = listener.onResponse(response)
    override fun parseNetworkResponse(response: NetworkResponse?) = response.extractResult()

    internal enum class WebauthnRequestParameters(val value: String) {
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

    companion object {
        private const val baseWPLoginUrl = "https://wordpress.com/wp-login.php?action"
        private const val challengeEndpoint = "webauthn-challenge-endpoint"
        private const val authEndpoint = "webauthn-authentication-endpoint"
        private const val WEBAUTHN_DATA = "data"

        internal const val webauthnChallengeEndpointUrl = "$baseWPLoginUrl=$challengeEndpoint"
        internal const val webauthnAuthEndpointUrl = "$baseWPLoginUrl=$authEndpoint"
        internal const val WEBAUTHN_AUTH_TYPE = "webauthn"
    }
}
