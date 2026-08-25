package org.wordpress.android.ui.sitecreation.usecases

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.DomainSuggestion
import uniffi.wp_api.DomainSuggestionsParams
import uniffi.wp_api.WpErrorCode
import javax.inject.Inject

const val FETCH_DOMAINS_VENDOR_DOT = "dot"
const val FETCH_DOMAINS_VENDOR_MOBILE = "mobile"
private const val FETCH_DOMAINS_SIZE = 20u
private const val ERROR_CODE_INVALID_QUERY = "invalid_query"
private const val ERROR_CODE_EMPTY_RESULTS = "empty_results"
private const val ERROR_TYPE_GENERIC = "GENERIC_ERROR"
private const val ERROR_TYPE_NO_ACCESS_TOKEN = "NO_ACCESS_TOKEN"

class FetchDomainsUseCase @Inject constructor(
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

    suspend fun fetchDomains(
        query: String,
        vendor: String,
        onlyWordpressCom: Boolean,
    ): FetchDomainsResult {
        val client = getOrCreateClient() ?: run {
            AppLog.e(
                AppLog.T.DOMAIN_REGISTRATION,
                "Cannot fetch domain suggestions without a WP.com access token"
            )
            return FetchDomainsResult.Error(type = ERROR_TYPE_NO_ACCESS_TOKEN, message = null)
        }
        val params = DomainSuggestionsParams(
            query = query,
            quantity = FETCH_DOMAINS_SIZE,
            vendor = vendor,
            onlyWordpressdotcom = onlyWordpressCom, // checkstyle ignore
            includeWordpressdotcom = true, // checkstyle ignore
            includeDotblogsubdomain = false,
        )
        return when (
            val result = client
                .request { it.domains().suggestions(params).data }
        ) {
            is WpRequestResult.Success ->
                FetchDomainsResult.Success(result.response)
            is WpRequestResult.WpError ->
                when (val code = result.apiErrorCode()) {
                    ERROR_CODE_INVALID_QUERY -> FetchDomainsResult.InvalidQuery
                    // The API reports "no domains for that search" as an error,
                    // but there is nothing wrong with the request.
                    ERROR_CODE_EMPTY_RESULTS -> FetchDomainsResult.Success(emptyList())
                    else -> FetchDomainsResult.Error(
                        type = code ?: ERROR_TYPE_GENERIC,
                        message = result.errorMessage,
                    )
                }
            else -> FetchDomainsResult.Error(type = ERROR_TYPE_GENERIC, message = null)
        }
    }
}

/**
 * The API's own error code, for the codes this endpoint defines. Codes the
 * WordPress REST API also uses are modelled as [WpErrorCode] variants and
 * return null.
 */
private fun WpRequestResult.WpError<*>.apiErrorCode(): String? =
    (errorCode as? WpErrorCode.CustomException)?.v1

sealed interface FetchDomainsResult {
    data class Success(
        val suggestions: List<DomainSuggestion>,
    ) : FetchDomainsResult

    data object InvalidQuery : FetchDomainsResult

    data class Error(
        val type: String,
        val message: String?,
    ) : FetchDomainsResult
}
