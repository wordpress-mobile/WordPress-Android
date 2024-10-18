package org.wordpress.android.fluxc.utils

import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError

object NetworkErrorMapper {
    fun <T> map(
        error: WPComGsonNetworkError,
        genericError: T,
        invalidResponse: T?,
        authorizationRequired: T? = null
    ): T {
        var errorType: T
        mapGenericNetworkError(error, genericError, invalidResponse).also { errorType = it }
        mapWPComGsonNetworkApiError(error, authorizationRequired)?.let { errorType = it }
        return errorType
    }

    private fun <T> mapGenericNetworkError(
        error: BaseNetworkError,
        genericError: T,
        invalidResponse: T? = null
    ): T {
        var errorType = genericError
        if (error.isGeneric) {
            if (error.type == BaseRequest.GenericErrorType.INVALID_RESPONSE && invalidResponse != null) {
                errorType = invalidResponse
            }
        }
        return errorType
    }

    private fun <T> mapWPComGsonNetworkApiError(
        error: WPComGsonNetworkError,
        authorizationRequired: T? = null
    ): T? {
        return if ("unauthorized" == error.apiError && authorizationRequired != null) {
            authorizationRequired
        } else {
            null
        }
    }
}
