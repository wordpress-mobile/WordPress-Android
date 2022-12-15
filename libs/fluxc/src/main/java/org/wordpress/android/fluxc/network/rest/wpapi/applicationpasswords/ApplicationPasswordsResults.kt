package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError

internal sealed interface ApplicationPasswordCreationResult {
    data class Existing(
        val credentials: ApplicationPasswordCredentials
    ) : ApplicationPasswordCreationResult

    data class Created(
        val credentials: ApplicationPasswordCredentials
    ) : ApplicationPasswordCreationResult

    data class NotSupported(val originalError: BaseNetworkError) : ApplicationPasswordCreationResult
    data class Failure(val error: BaseNetworkError) : ApplicationPasswordCreationResult
}

internal sealed interface ApplicationPasswordDeletionResult {
    object Success : ApplicationPasswordDeletionResult
    data class Failure(val error: BaseNetworkError) : ApplicationPasswordDeletionResult
}
