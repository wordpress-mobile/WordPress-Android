package org.wordpress.android.ui.domains.usecases

import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.DomainContactInformation
import javax.inject.Inject

class FetchDomainContactUseCase @Inject constructor(
    private val wpComApiClient: WpComApiClient,
) {
    /**
     * Fetches the WHOIS contact details the account registers domains with,
     * which prefill the registration form.
     */
    suspend fun execute(): DomainContactResult {
        val result = wpComApiClient
            .request { it.me().domainContactInformation().data }
        return when (result) {
            is WpRequestResult.Success -> DomainContactResult.Success(result.response)
            else -> {
                AppLog.e(
                    AppLog.T.API,
                    "An error occurred while fetching domain contact details"
                )
                DomainContactResult.Error(result.apiErrorMessage(), result.isDeviceOffline())
            }
        }
    }
}

sealed interface DomainContactResult {
    data class Success(
        val contact: DomainContactInformation,
    ) : DomainContactResult

    data class Error(
        val message: String? = null,
        val isDeviceOffline: Boolean = false,
    ) : DomainContactResult
}
