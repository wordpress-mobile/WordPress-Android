package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError

interface ApplicationPasswordGenerator {
    suspend fun createApplicationPassword(
        site: SiteModel,
        applicationName: String
    ): ApplicationPasswordCreationResult

    suspend fun deleteApplicationPassword(
        siteModel: SiteModel,
        applicationName: String
    ): ApplicationPasswordDeletionResult
}

sealed interface ApplicationPasswordCreationResult {
    data class Success(val password: String) : ApplicationPasswordCreationResult
    object NotSupported : ApplicationPasswordCreationResult
    data class Failure(val error: BaseNetworkError) : ApplicationPasswordCreationResult
}

sealed interface ApplicationPasswordDeletionResult {
    object Success : ApplicationPasswordDeletionResult
    data class Failure(val error: BaseNetworkError) : ApplicationPasswordDeletionResult
}