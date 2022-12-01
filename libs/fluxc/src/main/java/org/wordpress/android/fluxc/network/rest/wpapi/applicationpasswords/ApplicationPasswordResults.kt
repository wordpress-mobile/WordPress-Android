package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError

sealed interface ApplicationPasswordCreationResult {
    data class Success(val password: String) : ApplicationPasswordCreationResult
    object NotSupported : ApplicationPasswordCreationResult
    data class Failure(val error: BaseNetworkError) : ApplicationPasswordCreationResult
}

sealed interface ApplicationPasswordDeletionResult {
    object Success : ApplicationPasswordDeletionResult
    data class Failure(val error: BaseNetworkError) : ApplicationPasswordDeletionResult
}