package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

data class WPcomAuthorizationCodeResponse(
    val access_token: String,
    val token_type: String,
    val blog_id: String,
    val blog_url: String,
    val scope: String
)
