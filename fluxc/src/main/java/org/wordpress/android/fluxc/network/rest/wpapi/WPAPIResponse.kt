package org.wordpress.android.fluxc.network.rest.wpapi

import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError

sealed class WPAPIResponse<T> {
    data class Success<T>(val data: T) : WPAPIResponse<T>()
    data class Error<T>(val error: BaseNetworkError) : WPAPIResponse<T>()
}
