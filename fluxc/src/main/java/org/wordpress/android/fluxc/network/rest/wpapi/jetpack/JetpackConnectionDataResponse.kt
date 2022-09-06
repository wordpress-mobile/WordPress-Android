package org.wordpress.android.fluxc.network.rest.wpapi.jetpack

import com.google.gson.annotations.SerializedName

data class JetpackConnectionDataResponse(
    val currentUser: CurrentUser
)

data class CurrentUser(
    @SerializedName("isConnected") val isConnected: Boolean?,
    @SerializedName("isMaster") val isMaster: Boolean?,
    @SerializedName("username") val username: String?,
    @SerializedName("wpcomUser") val wpcomUser: WpcomUser?,
    @SerializedName("gravatar") val gravatar: String?,
    @SerializedName("permissions") val permissions: Map<String, Boolean>?
)

data class WpcomUser(
    @SerializedName("ID") val id: Long?,
    @SerializedName("login") val login: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("text_direction") val textDirection: String?,
    @SerializedName("site_count") val siteCount: Long?,
    @SerializedName("jetpack_connect") val jetpackConnect: String?,
    @SerializedName("avatar") val avatar: String?
)
