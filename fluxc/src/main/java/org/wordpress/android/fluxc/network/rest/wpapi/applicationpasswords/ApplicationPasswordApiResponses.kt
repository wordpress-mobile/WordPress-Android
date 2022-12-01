package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import com.google.gson.annotations.SerializedName

data class ApplicationPasswordCreationResponse(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("name") val name: String,
    @SerializedName("password") val password: String
)

data class ApplicationPasswordDeleteResponse(
    @SerializedName("deleted") val deleted: Boolean
)
