package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import com.google.gson.annotations.SerializedName

internal data class ApplicationPasswordCreationResponse(
    @SerializedName("uuid") val uuid: ApplicationPasswordUUID,
    @SerializedName("name") val name: String,
    @SerializedName("password") val password: String
)

internal data class ApplicationPasswordsFetchResponse(
    @SerializedName("uuid") val uuid: ApplicationPasswordUUID,
    @SerializedName("name") val name: String
)

internal data class ApplicationPasswordDeleteResponse(
    @SerializedName("deleted") val deleted: Boolean
)
