package org.wordpress.android.ui.sitecreation.usecases

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.networking.restapi.WpComApiClientProvider
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

class FetchDomainsUseCase @Inject constructor(
    private val wpComApiClientProvider: WpComApiClientProvider,
    private val accountStore: AccountStore,
) {
    private var wpComApiClient: WpComApiClient? = null

    @Synchronized
    private fun getOrCreateClient(): WpComApiClient {
        val token = requireNotNull(accountStore.accessToken) {
            "WP.com access token is required"
        }
        return wpComApiClient
            ?: wpComApiClientProvider.getWpComApiClient(token)
                .also { wpComApiClient = it }
    }

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
            val result = getOrCreateClient()
                .request { it.domains().suggestions(params).data }
        ) {
            is WpRequestResult.Success ->
                FetchDomainsResult.Success(query, result.response)
            is WpRequestResult.WpError ->
                if (result.isInvalidQuery()) {
                    FetchDomainsResult.InvalidQuery(query)
                } else {
                    FetchDomainsResult.Error(query)
                }
            else -> FetchDomainsResult.Error(query)
        }
    }
}

private fun WpRequestResult.WpError<*>.isInvalidQuery(): Boolean {
    val code = errorCode
    return code is WpErrorCode.CustomException &&
        code.v1 == ERROR_CODE_INVALID_QUERY
}

sealed interface FetchDomainsResult {
    val query: String

    data class Success(
        override val query: String,
        val suggestions: List<DomainSuggestion>,
    ) : FetchDomainsResult

    data class InvalidQuery(
        override val query: String,
    ) : FetchDomainsResult

    data class Error(
        override val query: String,
    ) : FetchDomainsResult
}
