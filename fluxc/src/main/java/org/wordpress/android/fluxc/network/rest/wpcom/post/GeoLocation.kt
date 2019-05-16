package org.wordpress.android.fluxc.network.rest.wpcom.post

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.rest.JsonObjectOrFalse

data class GeoLocation(
    @SerializedName("latitude") val latitude: Double = 0.0,
    @SerializedName("longitude") val longitude: Double = 0.0,
    @SerializedName("address") val address: String? = null
) : JsonObjectOrFalse()
