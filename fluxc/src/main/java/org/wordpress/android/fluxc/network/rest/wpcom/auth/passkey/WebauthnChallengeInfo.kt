package org.wordpress.android.fluxc.network.rest.wpcom.auth.passkey

data class WebauthnChallengeInfo(
    val challenge: String,
    val rpId: String,
    val twoStepNonce: String,
    val allowCredentials: List<WebauthnCredential>,
    val timeout: Int
)

data class WebauthnCredential(
    val type: String,
    val id: String,
    val transports: List<String>
)
