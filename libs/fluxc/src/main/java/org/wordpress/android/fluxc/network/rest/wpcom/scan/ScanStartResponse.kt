package org.wordpress.android.fluxc.network.rest.wpcom.scan

data class ScanStartResponse(
    val error: Error? = null,
    val success: Boolean
) {
    data class Error(
        val error_data: List<Any>,
        val errors: Errors
    )

    data class Errors(
        val vp_api_error: List<String>
    )
}
