package org.wordpress.android.ui.domains.management.newdomainsearch.domainsfetcher

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.Constants
import org.wordpress.android.fluxc.model.products.Product
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse
import org.wordpress.android.fluxc.store.ProductsStore
import org.wordpress.android.fluxc.store.SiteStore

@Suppress("MaxLineLength")
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class NewDomainsSearchRepositoryTest : BaseUnitTest() {
    @Mock
    private lateinit var productsStore: ProductsStore

    @Mock
    private lateinit var suggestedDomainsFetcher: SuggestedDomainsFetcher

    @InjectMocks
    private lateinit var repository: NewDomainsSearchRepository

    @Test
    fun `GIVEN successfully fetched products with sale price available and suggestions WHEN searchForDomains THEN return DomainsResult Success with suggestions`() =
        test {
            mockProductWithSaleResponse()
            mockSuccessfulDomainFetchResponse()

            val result = repository.searchForDomains("query")

            assertThat(result).isEqualTo(
                NewDomainsSearchRepository.DomainsResult.Success(
                    proposedDomains = listOf(
                        ProposedDomain(
                            productId = 0,
                            domain = "example.com",
                            price = "USD 50",
                            salePrice = "USD 10",
                            supportsPrivacy = true
                        )
                    )
                )
            )
        }

    @Test
    fun `GIVEN product with sale does not appear in domains fetch response WHEN searchForDomains THEN return DomainsResult Success with suggestions with no sale`() =
        test {
            mockProductWithSaleResponse(productId = 1)
            mockSuccessfulDomainFetchResponse()

            val result = repository.searchForDomains("query")

            assertThat(result).isEqualTo(
                NewDomainsSearchRepository.DomainsResult.Success(
                    proposedDomains = listOf(
                        ProposedDomain(
                            productId = 0,
                            domain = "example.com",
                            price = "USD 50",
                            salePrice = null,
                            supportsPrivacy = true
                        )
                    )
                )
            )
        }

    @Suppress("LongMethod")
    @Test
    fun `GIVEN few domains with different relevance WHEN searchForDomains THEN sort domains by descending by relevance`() =
        test {
            mockFetchProductsError()
            whenever(suggestedDomainsFetcher.fetch(any())).thenReturn(
                SiteStore.OnSuggestedDomains(
                    query = "query",
                    suggestions = listOf(
                        DomainSuggestionResponse().apply {
                            product_id = 0
                            domain_name = "first.com"
                            is_free = false
                            relevance = 1f
                            cost = "USD 30"
                            supports_privacy = true
                        },
                        DomainSuggestionResponse().apply {
                            product_id = 1
                            domain_name = "second.com"
                            is_free = false
                            relevance = 2f
                            cost = "USD 40"
                            supports_privacy = true
                        },
                        DomainSuggestionResponse().apply {
                            product_id = 2
                            domain_name = "third.com"
                            is_free = false
                            relevance = 0f
                            cost = "USD 50"
                            supports_privacy = true
                        },
                    )
                )
            )

            val result = repository.searchForDomains("query")

            assertThat(result).isEqualTo(
                NewDomainsSearchRepository.DomainsResult.Success(
                    proposedDomains = listOf(
                        ProposedDomain(
                            productId = 1,
                            domain = "second.com",
                            price = "USD 40",
                            salePrice = null,
                            supportsPrivacy = true
                        ),
                        ProposedDomain(
                            productId = 0,
                            domain = "first.com",
                            price = "USD 30",
                            salePrice = null,
                            supportsPrivacy = true
                        ),
                        ProposedDomain(productId = 2,
                            domain = "third.com",
                            price = "USD 50",
                            salePrice = null,
                            supportsPrivacy = true
                        ),
                    )
                )
            )
        }

    @Test
    fun `GIVEN few domains with a free domain WHEN searchForDomains THEN filter out free domains`() =
        test {
            mockFetchProductsError()
            whenever(suggestedDomainsFetcher.fetch(any())).thenReturn(
                SiteStore.OnSuggestedDomains(
                    query = "query",
                    suggestions = listOf(
                        DomainSuggestionResponse().apply {
                            product_id = 0
                            domain_name = "first.com"
                            is_free = false
                            relevance = 1f
                            cost = "USD 30"
                            supports_privacy = true
                        },
                        DomainSuggestionResponse().apply {
                            product_id = 1
                            domain_name = "second.com"
                            is_free = false
                            relevance = 2f
                            cost = "USD 40"
                            supports_privacy = true
                        },
                        DomainSuggestionResponse().apply {
                            product_id = 2
                            domain_name = "third.com"
                            is_free = true
                            relevance = 0f
                            cost = "USD 50"
                            supports_privacy = true
                        },
                    )
                )
            )

            val result = repository.searchForDomains("query")

            assertThat(result).isEqualTo(
                NewDomainsSearchRepository.DomainsResult.Success(
                    proposedDomains = listOf(
                        ProposedDomain(
                            productId = 1,
                            domain = "second.com",
                            price = "USD 40",
                            salePrice = null,
                            supportsPrivacy = true
                        ),
                        ProposedDomain(
                            productId = 0,
                            domain = "first.com",
                            price = "USD 30",
                            salePrice = null,
                            supportsPrivacy = true
                        ),
                    )
                )
            )
        }

    @Test
    fun `GIVEN product fetch with error and successful suggestions response WHEN searchForDomains THEN return DomainsResult Success with suggestions with no sale`() =
        test {
            mockFetchProductsError()
            mockSuccessfulDomainFetchResponse()

            val result = repository.searchForDomains("query")

            assertThat(result).isEqualTo(
                NewDomainsSearchRepository.DomainsResult.Success(
                    proposedDomains = listOf(
                        ProposedDomain(
                            productId = 0,
                            domain = "example.com",
                            price = "USD 50",
                            salePrice = null,
                            supportsPrivacy = true
                        )
                    )
                )
            )
        }

    @Test
    fun `GIVEN product fetch with error and suggestions with error WHEN searchForDomains THEN return DomainsResult Error`() =
        test {
            mockFetchProductsError()
            mockDomainsFetchError()

            val result = repository.searchForDomains("query")

            assertThat(result).isEqualTo(NewDomainsSearchRepository.DomainsResult.Error)
        }

    @Test
    fun `GIVEN two search calls WHEN searchForDomains THEN fetch products only once`() =
        test {
            mockProductWithSaleResponse()
            mockSuccessfulDomainFetchResponse("incorrect query")

            val result = repository.searchForDomains("query")

            assertThat(result).isEqualTo(NewDomainsSearchRepository.DomainsResult.Error)
        }

    @Test
    fun `GIVEN event query is not equal to onDomainSuggestionFetched query argument WHEN searchForDomains THEN return error`() =
        test {
            mockProductWithSaleResponse()
            mockSuccessfulDomainFetchResponse()

            repository.searchForDomains("query")
            repository.searchForDomains("query")

            verify(productsStore, times(1)).fetchProducts(Constants.TYPE_DOMAINS_PRODUCT)
        }

    private suspend fun mockProductWithSaleResponse(productId: Int = 0) {
        whenever(productsStore.fetchProducts(Constants.TYPE_DOMAINS_PRODUCT)).thenReturn(
            ProductsStore.OnProductsFetched(
                products = listOf(Product(productId = productId, combinedSaleCostDisplay = "USD 10",))
            )
        )
    }

    private suspend fun mockFetchProductsError() {
        whenever(productsStore.fetchProducts(Constants.TYPE_DOMAINS_PRODUCT)).thenReturn(
            ProductsStore.OnProductsFetched(error = mock())
        )
    }

    private suspend fun mockSuccessfulDomainFetchResponse(
        query: String = "query",
        productId: Int = 0,
        domainName: String = "example.com",
        isFree: Boolean = false,
    ) {
        whenever(suggestedDomainsFetcher.fetch(any())).thenReturn(
            SiteStore.OnSuggestedDomains(
                query = query,
                suggestions = listOf(
                    DomainSuggestionResponse().apply {
                        product_id = productId
                        domain_name = domainName
                        is_free = isFree
                        relevance = 0f
                        cost = "USD 50"
                        supports_privacy = true
                    }
                )
            )
        )
    }

    private suspend fun mockDomainsFetchError() {
        whenever(suggestedDomainsFetcher.fetch(any())).thenReturn(
            SiteStore.OnSuggestedDomains("query", emptyList()).apply { error = mock() }
        )
    }
}
