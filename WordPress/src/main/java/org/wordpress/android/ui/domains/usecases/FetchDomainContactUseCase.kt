package org.wordpress.android.ui.domains.usecases

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.DomainContactInformation
import javax.inject.Inject

class FetchDomainContactUseCase @Inject constructor(
    private val wpComApiClientProvider: WpComApiClientProvider,
    private val accountStore: AccountStore,
) {
    private var wpComApiClient: WpComApiClient? = null

    /**
     * Null when there is no WordPress.com account to make the request as.
     *
     * `AccountStore.accessToken` is typed nullable but reads `""` when signed
     * out, and is only null between an in-process sign out and the next
     * launch, so both have to be treated as no token.
     */
    @Synchronized
    private fun getOrCreateClient(): WpComApiClient? {
        val token = accountStore.accessToken?.takeIf { it.isNotEmpty() } ?: return null
        return wpComApiClient
            ?: wpComApiClientProvider.getWpComApiClient(token)
                .also { wpComApiClient = it }
    }

    /**
     * Fetches the WHOIS contact details the account registers domains with,
     * which prefill the registration form.
     */
    suspend fun execute(): DomainContactResult {
        val client = getOrCreateClient() ?: run {
            AppLog.e(
                AppLog.T.API,
                "Cannot fetch domain contact details without a WP.com access token"
            )
            return DomainContactResult.Error()
        }
        val result = client
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
