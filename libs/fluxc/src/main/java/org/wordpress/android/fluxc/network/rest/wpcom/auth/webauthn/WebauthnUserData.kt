package org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn

data class WebauthnUserData(
    val userId: Long,
    val webauthnNonce: String
)
