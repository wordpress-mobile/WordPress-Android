package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import com.google.gson.annotations.SerializedName

data class WPcomAuthorizationCodeResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("blog_id") val blogId: String,
    @SerializedName("blog_url") val blogUrl: String,
    val scope: String
)
