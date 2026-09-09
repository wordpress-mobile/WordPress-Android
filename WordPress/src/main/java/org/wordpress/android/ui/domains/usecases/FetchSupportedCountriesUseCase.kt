package org.wordpress.android.ui.domains.usecases

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.SupportedCountries
import uniffi.wp_api.SupportedCountry
import javax.inject.Inject

class FetchSupportedCountriesUseCase @Inject constructor(
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
     * Fetches the countries a domain purchase can be billed to.
     */
    suspend fun execute(): SupportedCountriesResult {
        val client = getOrCreateClient() ?: run {
            AppLog.e(
                AppLog.T.API,
                "Cannot fetch supported countries without a WP.com access token"
            )
            return SupportedCountriesResult.Error()
        }
        val result = client
            .request { it.me().transactionsSupportedCountries().data }
        return when (result) {
            is WpRequestResult.Success -> SupportedCountriesResult.Success(result.response.asPickerList())
            else -> {
                AppLog.e(
                    AppLog.T.API,
                    "An error occurred while fetching supported countries"
                )
                SupportedCountriesResult.Error(result.apiErrorMessage())
            }
        }
    }
}

/**
 * The countries in the order the picker lists them.
 *
 * The API sends one flat array in three groups, separated by entries whose code
 * and name are empty: the account's own country, then a block of common ones,
 * then every country alphabetically. wordpress-rs reads those separators and
 * splits at the first of them, so [SupportedCountries.featured] holds the
 * account's country and [SupportedCountries.all] holds the rest. Concatenating
 * them restores the array the API sent, minus the separators.
 *
 * A country therefore appears more than once, which is what the picker has
 * always shown: the account's country and the common ones sit at the top, and
 * the alphabetical run below repeats them.
 */
private fun SupportedCountries.asPickerList(): List<SupportedCountry> = featured + all

sealed interface SupportedCountriesResult {
    data class Success(
        val countries: List<SupportedCountry>,
    ) : SupportedCountriesResult

    data class Error(val message: String? = null) : SupportedCountriesResult
}
