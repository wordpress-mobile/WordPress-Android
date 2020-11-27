package org.wordpress.android.fluxc.network.rest.wpcom.scan

import com.google.gson.annotations.SerializedName

data class ScanStartResponse(
    @SerializedName("error") val error: Error? = null,
    @SerializedName("success") val success: Boolean?
) {
    data class Error(
        @SerializedName("error_data") val errorData: List<Any>?,
        @SerializedName("errors") val errors: Errors?
    )

    data class Errors(
        @SerializedName("vp_api_error") val vpApiError: List<String>?
    )
}
