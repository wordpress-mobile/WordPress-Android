package org.wordpress.android.ui.domains.usecases

import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.SupportedCountries
import uniffi.wp_api.SupportedCountry
import javax.inject.Inject

class FetchSupportedCountriesUseCase @Inject constructor(
    private val wpComApiClient: WpComApiClient,
) {
    /**
     * Fetches the countries a domain purchase can be billed to.
     */
    suspend fun execute(): SupportedCountriesResult {
        val result = wpComApiClient
            .request { it.me().transactionsSupportedCountries().data }
        return when (result) {
            is WpRequestResult.Success -> SupportedCountriesResult.Success(result.response.asPickerList())
            else -> {
                AppLog.e(
                    AppLog.T.API,
                    "An error occurred while fetching supported countries"
                )
                SupportedCountriesResult.Error(result.apiErrorMessage(), result.isDeviceOffline())
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
 * A country therefore appears more than once: the account's country and the
 * common ones sit at the top, and the alphabetical run below repeats them.
 */
private fun SupportedCountries.asPickerList(): List<SupportedCountry> = featured + all

sealed interface SupportedCountriesResult {
    data class Success(
        val countries: List<SupportedCountry>,
    ) : SupportedCountriesResult

    data class Error(
        val message: String? = null,
        val isDeviceOffline: Boolean = false,
    ) : SupportedCountriesResult
}
