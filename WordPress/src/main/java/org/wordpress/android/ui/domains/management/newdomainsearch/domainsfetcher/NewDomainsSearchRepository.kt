package org.wordpress.android.ui.domains.management.newdomainsearch.domainsfetcher

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.Constants
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.products.Product
import org.wordpress.android.fluxc.store.ProductsStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val SUGGESTIONS_REQUEST_COUNT = 20

class NewDomainsSearchRepository @Inject constructor(
    private val productsStore: ProductsStore,
    private val dispatcher: Dispatcher
) {
    var products: List<Product>? = null
    var suggestions: DomainsResult = DomainsResult.Empty

    suspend fun searchForDomains(query: String): DomainsResult {
        if (products == null) fetchProducts()
        return suspendCoroutine { continuation ->
            val suggestionsFetchedCallback = onDomainSuggestionsFetchedCallback(query, continuation)
            dispatcher.register(suggestionsFetchedCallback)
            fetchDomainsSuggestions(query)
        }
    }

    private suspend fun fetchProducts() {
        val result = productsStore.fetchProducts(Constants.TYPE_DOMAINS_PRODUCT)
        if (!result.isError) result.products?.let { products = it }
    }

    private fun onDomainSuggestionsFetchedCallback(
        query: String,
        continuation: Continuation<DomainsResult>
    ) = object {
        @Suppress("unused")
        @Subscribe(threadMode = ThreadMode.MAIN)
        fun onDomainSuggestionsFetched(event: OnSuggestedDomains) {
            if (query != event.query || event.isError) {
                continuation.resume(DomainsResult.Error)
            } else {
                val suggestions = event.suggestions
                    .sortedBy { it.relevance }
                    .map { domain ->
                        val product = products?.firstOrNull { product -> product.productId == domain.product_id }
                        val splitDomainName = domain.domain_name.split('.')
                        val suffix = splitDomainName.last()
                        val prefix = domain.domain_name.removeSuffix(suffix)
                        NewDomain(
                            productId = domain.product_id,
                            domainPrefix = prefix,
                            domainSuffix = suffix,
                            price = domain.cost,
                            salePrice = product?.combinedSaleCostDisplay,
                        )
                    }
                    .asReversed()
                continuation.resume(DomainsResult.Success(suggestions))
            }
            dispatcher.unregister(this)
        }
    }

    private fun fetchDomainsSuggestions(query: String) {
        val suggestDomainsPayload = SiteStore.SuggestDomainsPayload(
            query,
            onlyWordpressCom = false,
            includeWordpressCom = false,
            includeDotBlogSubdomain = true,
            quantity = SUGGESTIONS_REQUEST_COUNT
        )
        dispatcher.dispatch(SiteActionBuilder.newSuggestDomainsAction(suggestDomainsPayload))
    }

    sealed interface DomainsResult {
        data class Success(val suggestions: List<NewDomain>) : DomainsResult
        object Empty : DomainsResult
        object Error : DomainsResult
    }
}
