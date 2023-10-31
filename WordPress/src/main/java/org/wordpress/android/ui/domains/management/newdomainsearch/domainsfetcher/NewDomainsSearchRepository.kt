package org.wordpress.android.ui.domains.management.newdomainsearch.domainsfetcher

import org.wordpress.android.Constants
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.products.Product
import org.wordpress.android.fluxc.store.ProductsStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains
import javax.inject.Inject

private const val SUGGESTIONS_REQUEST_COUNT = 20

class NewDomainsSearchRepository @Inject constructor(
    private val productsStore: ProductsStore,
    private val suggestedDomainsFetcher: SuggestedDomainsFetcher
) {
    var products: List<Product>? = null

    suspend fun searchForDomains(query: String): DomainsResult {
        if (products == null) fetchProducts()
        return SiteActionBuilder.newSuggestDomainsAction(
            SiteStore.SuggestDomainsPayload(
                query = query,
                onlyWordpressCom = false,
                includeWordpressCom = false,
                includeDotBlogSubdomain = false,
                quantity = SUGGESTIONS_REQUEST_COUNT
            )
        ).let { action ->
            suggestedDomainsFetcher.fetch(action)
        }.let { event ->
            onDomainSuggestionsFetched(query, event)
        }
    }

    private suspend fun fetchProducts() {
        val result = productsStore.fetchProducts(Constants.TYPE_DOMAINS_PRODUCT)
        if (!result.isError) result.products?.let { products = it }
    }

    private fun onDomainSuggestionsFetched(query: String, event: OnSuggestedDomains): DomainsResult {
        return if (query == event.query && !event.isError) {
            val suggestions = event.suggestions
                .filter { !it.is_free }
                .sortedBy { it.relevance }
                .map { domain ->
                    val product = products?.firstOrNull { product -> product.productId == domain.product_id }
                    val splitDomainName = domain.domain_name.split('.')
                    val suffix = splitDomainName.last()
                    val prefix = domain.domain_name.removeSuffix(suffix)
                    ProposedDomain(
                        productId = domain.product_id,
                        domainPrefix = prefix,
                        domainSuffix = suffix,
                        price = domain.cost,
                        salePrice = product?.combinedSaleCostDisplay,
                    )
                }
                .asReversed()
            DomainsResult.Success(suggestions)
        } else {
            DomainsResult.Error
        }
    }

    sealed interface DomainsResult {
        data class Success(val proposedDomains: List<ProposedDomain>) : DomainsResult
        object Error : DomainsResult
    }
}
