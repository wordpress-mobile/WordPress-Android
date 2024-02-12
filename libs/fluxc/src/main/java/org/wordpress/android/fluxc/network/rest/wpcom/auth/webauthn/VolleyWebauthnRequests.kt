package org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn

import com.android.volley.Response
import com.android.volley.Response.ErrorListener
import com.google.gson.annotations.SerializedName
import org.json.JSONObject
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.BaseWebauthnRequest.WebauthnRequestParameters.AUTH_TYPE
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.BaseWebauthnRequest.WebauthnRequestParameters.CLIENT_DATA
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.BaseWebauthnRequest.WebauthnRequestParameters.CLIENT_ID
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.BaseWebauthnRequest.WebauthnRequestParameters.CLIENT_SECRET
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.BaseWebauthnRequest.WebauthnRequestParameters.CREATE_2FA_COOKIES_ONLY
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.BaseWebauthnRequest.WebauthnRequestParameters.GET_BEARER_TOKEN
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.BaseWebauthnRequest.WebauthnRequestParameters.TWO_STEP_NONCE
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.BaseWebauthnRequest.WebauthnRequestParameters.USER_ID

class WebauthnChallengeRequest(
    userId: String,
    twoStepNonce: String,
    clientId: String,
    clientSecret: String,
    listener: Response.Listener<JSONObject>,
    errorListener: ErrorListener
): BaseWebauthnRequest<JSONObject>(webauthnChallengeEndpointUrl, errorListener, listener) {
    override val parameters: Map<String, String> = mapOf(
        CLIENT_ID.value to clientId,
        CLIENT_SECRET.value to clientSecret,
        USER_ID.value to userId,
        AUTH_TYPE.value to WEBAUTHN_AUTH_TYPE,
        TWO_STEP_NONCE.value to twoStepNonce
    )

    override fun serializeResponse(response: String) = JSONObject(response)
}

@SuppressWarnings("LongParameterList")
class WebauthnTokenRequest(
    userId: String,
    twoStepNonce: String,
    clientId: String,
    clientSecret: String,
    clientData: String,
    listener: Response.Listener<WebauthnToken>,
    errorListener: ErrorListener
) : BaseWebauthnRequest<WebauthnToken>(webauthnAuthEndpointUrl, errorListener, listener) {
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

    override fun serializeResponse(response: String): WebauthnToken =
        gson.fromJson(response, WebauthnToken::class.java)
}

class WebauthnToken(
    @SerializedName("bearer_token")
    val bearerToken: String
)
