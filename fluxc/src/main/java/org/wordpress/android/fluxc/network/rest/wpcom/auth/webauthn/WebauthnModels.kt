package org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn

import com.google.gson.annotations.SerializedName

class WebauthnToken(
    @SerializedName("bearer_token")
    val bearerToken: String
)
