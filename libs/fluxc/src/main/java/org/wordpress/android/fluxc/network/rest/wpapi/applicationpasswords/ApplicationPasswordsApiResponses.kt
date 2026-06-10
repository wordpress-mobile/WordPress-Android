package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import com.google.gson.annotations.SerializedName

// Note: Gson populates these fields via reflection and can assign null to a non-null Kotlin
// property if the server omits it. uuid and name are kept nullable and guarded at the usage
// sites to avoid a latent NPE.
internal data class ApplicationPasswordCreationResponse(
    @SerializedName("uuid") val uuid: ApplicationPasswordUUID?,
    @SerializedName("name") val name: String?,
    @SerializedName("password") val password: String
)

internal data class ApplicationPasswordsFetchResponse(
    @SerializedName("uuid") val uuid: ApplicationPasswordUUID?,
    @SerializedName("name") val name: String?
)

internal data class ApplicationPasswordDeleteResponse(
    @SerializedName("deleted") val deleted: Boolean
)
