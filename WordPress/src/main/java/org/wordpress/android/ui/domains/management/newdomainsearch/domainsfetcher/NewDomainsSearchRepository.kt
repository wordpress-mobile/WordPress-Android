package org.wordpress.android.ui.domains.management.newdomainsearch.domainsfetcher

import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.DomainSuggestion
import uniffi.wp_api.DomainSuggestionsParams
import uniffi.wp_api.Product
import uniffi.wp_api.ProductTypeFilter
import uniffi.wp_api.ProductsParams
import javax.inject.Inject

private const val SUGGESTIONS_REQUEST_COUNT = 20u

class NewDomainsSearchRepository @Inject constructor(
    private val wpComApiClient: WpComApiClient,
) {
    private var products: List<Product>? = null

    suspend fun searchForDomains(query: String): DomainsResult {
        if (products == null) fetchProducts()

        val params = DomainSuggestionsParams(
            query = query,
            quantity = SUGGESTIONS_REQUEST_COUNT,
            onlyWordpressdotcom = false, // checkstyle ignore
            includeWordpressdotcom = false, // checkstyle ignore
            includeDotblogsubdomain = false,
        )

        return when (
            val result = wpComApiClient
                .request { it.domains().suggestions(params).data }
        ) {
            is WpRequestResult.Success -> {
                val suggestions = result.response
                    .filterIsInstance<DomainSuggestion.Paid>()
                    .sortedByDescending { it.v1.relevance }
                    .map { paid ->
                        val product = products?.firstOrNull {
                            it.productId == paid.v1.productId
                        }
                        ProposedDomain(
                            productId = paid.v1.productId.toInt(),
                            domain = paid.v1.domainName,
                            price = paid.v1.cost,
                            salePrice = product?.combinedSaleCostDisplay,
                            supportsPrivacy = paid.v1.supportsPrivacy,
                        )
                    }
                DomainsResult.Success(suggestions)
            }
            else -> DomainsResult.Error
        }
    }

    private suspend fun fetchProducts() {
        val params = ProductsParams(
            productType = ProductTypeFilter.Domains
        )
        val result = wpComApiClient
            .request { it.products().list(params).data }
        if (result is WpRequestResult.Success) {
            products = result.response.values.toList()
        }
    }

    sealed interface DomainsResult {
        data class Success(
            val proposedDomains: List<ProposedDomain>
        ) : DomainsResult
        data object Error : DomainsResult
    }
}
