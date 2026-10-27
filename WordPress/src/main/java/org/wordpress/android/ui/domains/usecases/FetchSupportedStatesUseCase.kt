package org.wordpress.android.ui.domains.usecases

import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.SupportedState
import javax.inject.Inject

class FetchSupportedStatesUseCase @Inject constructor(
    private val wpComApiClient: WpComApiClient,
) {
    /**
     * Fetches the subdivisions a country registers domains against. Empty for
     * the countries that have none, which is what leaves the state field
     * disabled.
     */
    suspend fun execute(countryCode: String): SupportedStatesResult {
        val result = wpComApiClient
            .request { it.domains().supportedStates(countryCode).data }
        return when (result) {
            is WpRequestResult.Success -> SupportedStatesResult.Success(result.response)
            else -> {
                AppLog.e(
                    AppLog.T.API,
                    "An error occurred while fetching supported states"
                )
                SupportedStatesResult.Error(result.apiErrorMessage(), result.isDeviceOffline())
            }
        }
    }
}

sealed interface SupportedStatesResult {
    data class Success(
        val states: List<SupportedState>,
    ) : SupportedStatesResult

    data class Error(
        val message: String? = null,
        val isDeviceOffline: Boolean = false,
    ) : SupportedStatesResult
}
