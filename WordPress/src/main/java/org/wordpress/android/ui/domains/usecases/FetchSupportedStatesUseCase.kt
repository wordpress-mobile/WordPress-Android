package org.wordpress.android.ui.domains.usecases

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.SupportedState
import javax.inject.Inject

class FetchSupportedStatesUseCase @Inject constructor(
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
     * Fetches the subdivisions a country registers domains against. Empty for
     * the countries that have none, which is what leaves the state field
     * disabled.
     */
    suspend fun execute(countryCode: String): SupportedStatesResult {
        val client = getOrCreateClient() ?: run {
            AppLog.e(
                AppLog.T.API,
                "Cannot fetch supported states without a WP.com access token"
            )
            return SupportedStatesResult.Error()
        }
        val result = client
            .request { it.domains().supportedStates(countryCode).data }
        return when (result) {
            is WpRequestResult.Success -> SupportedStatesResult.Success(result.response)
            else -> {
                AppLog.e(
                    AppLog.T.API,
                    "An error occurred while fetching supported states"
                )
                SupportedStatesResult.Error(result.apiErrorMessage())
            }
        }
    }
}

sealed interface SupportedStatesResult {
    data class Success(
        val states: List<SupportedState>,
    ) : SupportedStatesResult

    data class Error(val message: String? = null) : SupportedStatesResult
}
