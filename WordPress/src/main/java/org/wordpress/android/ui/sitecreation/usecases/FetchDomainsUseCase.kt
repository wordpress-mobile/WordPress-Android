package org.wordpress.android.ui.sitecreation.usecases

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

class FetchDomainsUseCase @Inject constructor(
    private val wpComApiClient: WpComApiClient,
) {
    suspend fun fetchDomains(
        query: String,
        vendor: String,
        onlyWordpressCom: Boolean,
    ): FetchDomainsResult {
        val params = DomainSuggestionsParams(
            query = query,
            quantity = FETCH_DOMAINS_SIZE,
            vendor = vendor,
            onlyWordpressdotcom = onlyWordpressCom, // checkstyle ignore
            includeWordpressdotcom = true, // checkstyle ignore
            includeDotblogsubdomain = false,
        )
        return when (
            val result = wpComApiClient
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
