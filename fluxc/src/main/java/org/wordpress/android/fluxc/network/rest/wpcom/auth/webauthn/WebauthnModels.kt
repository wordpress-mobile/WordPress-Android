package org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn

import com.google.gson.annotations.SerializedName

class WebauthnChallengeInfo(
    val challenge: String,
    val rpId: String,
    val allowCredentials: List<WebauthnCredentialResponse>,
    val timeout: Int,
    @SerializedName("two_step_nonce")
    val twoStepNonce: String
)

class WebauthnCredentialResponse(
    val type: String,
    val id: String,
    val transports: List<String>
)

class WebauthnToken(
    @SerializedName("bearer_token")
    val bearerToken: String
)
