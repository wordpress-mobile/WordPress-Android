package org.wordpress.android.fluxc.network.rest.wpcom.auth.passkey

data class WebauthnUserData(
    val userId: Long,
    val webauthnNonce: String
)
