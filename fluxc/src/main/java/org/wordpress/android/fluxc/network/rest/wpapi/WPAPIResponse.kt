package org.wordpress.android.fluxc.network.rest.wpapi

sealed class WPAPIResponse<T> {
    data class Success<T>(val data: T?) : WPAPIResponse<T>()
    data class Error<T>(val error: WPAPINetworkError) : WPAPIResponse<T>()
}
