package org.wordpress.android.ui.domains.management.newdomainsearch.domainsfetcher

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.DomainSuggestion
import uniffi.wp_api.PaidDomainSuggestion
import uniffi.wp_api.Product
import uniffi.wp_api.RequestMethod

@Suppress("MaxLineLength")
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class NewDomainsSearchRepositoryTest : BaseUnitTest() {
    @Mock
    private lateinit var wpComApiClientProvider: WpComApiClientProvider

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var wpComApiClient: WpComApiClient

    private lateinit var repository: NewDomainsSearchRepository

    @Before
    fun setUp() {
        whenever(accountStore.accessToken).thenReturn("test-token")
        whenever(wpComApiClientProvider.getWpComApiClient("test-token"))
            .thenReturn(wpComApiClient)
        repository = NewDomainsSearchRepository(
            wpComApiClientProvider,
            accountStore
        )
    }

    @Test
    fun `GIVEN product with sale WHEN searchForDomains THEN sale price comes from product`() =
        test {
            mockProductsThenSuggestions(
                products = mapOf(
                    "domain_reg" to testProduct(
                        productId = 6u,
                        combinedSaleCostDisplay = "$7.00"
                    )
                ),
                suggestions = listOf(
                    DomainSuggestion.Paid(
                        paidSuggestion(
                            domainName = "example.com",
                            cost = "\$50.00",
                            productId = 6u,
                        )
                    )
                )
            )

            val result = repository.searchForDomains("query")

            val success = result as NewDomainsSearchRepository.DomainsResult.Success
            assertThat(success.proposedDomains).hasSize(1)
            assertThat(success.proposedDomains[0].domain).isEqualTo("example.com")
            assertThat(success.proposedDomains[0].price).isEqualTo("\$50.00")
            assertThat(success.proposedDomains[0].salePrice).isEqualTo("$7.00")
        }

    @Test
    fun `GIVEN product without sale WHEN searchForDomains THEN sale price is null`() =
        test {
            mockProductsThenSuggestions(
                products = mapOf(
                    "domain_reg" to testProduct(
                        productId = 6u,
                        combinedSaleCostDisplay = null
                    )
                ),
                suggestions = listOf(
                    DomainSuggestion.Paid(
                        paidSuggestion(
                            domainName = "example.com",
                            cost = "\$50.00",
                            productId = 6u,
                        )
                    )
                )
            )

            val result = repository.searchForDomains("query")

            val success = result as NewDomainsSearchRepository.DomainsResult.Success
            assertThat(success.proposedDomains[0].salePrice).isNull()
        }

    @Test
    fun `GIVEN no matching product WHEN searchForDomains THEN sale price is null`() =
        test {
            mockProductsThenSuggestions(
                products = mapOf(
                    "other_product" to testProduct(
                        productId = 99u,
                        combinedSaleCostDisplay = "$5.00"
                    )
                ),
                suggestions = listOf(
                    DomainSuggestion.Paid(
                        paidSuggestion(
                            domainName = "example.com",
                            productId = 6u,
                        )
                    )
                )
            )

            val result = repository.searchForDomains("query")

            val success = result as NewDomainsSearchRepository.DomainsResult.Success
            assertThat(success.proposedDomains[0].salePrice).isNull()
        }

    @Test
    fun `GIVEN domains with different relevance WHEN searchForDomains THEN sort descending by relevance`() =
        test {
            mockProductsThenSuggestions(
                suggestions = listOf(
                    DomainSuggestion.Paid(
                        paidSuggestion(domainName = "first.com", relevance = 1.0)
                    ),
                    DomainSuggestion.Paid(
                        paidSuggestion(domainName = "second.com", relevance = 2.0)
                    ),
                    DomainSuggestion.Paid(
                        paidSuggestion(domainName = "third.com", relevance = 0.0)
                    ),
                )
            )

            val result = repository.searchForDomains("query")

            val success = result as NewDomainsSearchRepository.DomainsResult.Success
            assertThat(success.proposedDomains.map { it.domain })
                .containsExactly("second.com", "first.com", "third.com")
        }

    @Test
    fun `GIVEN mix of free and paid domains WHEN searchForDomains THEN filter out free domains`() =
        test {
            mockProductsThenSuggestions(
                suggestions = listOf(
                    DomainSuggestion.Paid(
                        paidSuggestion(domainName = "paid.com", relevance = 1.0)
                    ),
                    DomainSuggestion.Free(
                        uniffi.wp_api.FreeDomainSuggestion(
                            domainName = "free.wordpress.com",
                            cost = "Free",
                            isFree = true
                        )
                    ),
                )
            )

            val result = repository.searchForDomains("query")

            val success = result as NewDomainsSearchRepository.DomainsResult.Success
            assertThat(success.proposedDomains).hasSize(1)
            assertThat(success.proposedDomains[0].domain).isEqualTo("paid.com")
        }

    @Test
    fun `GIVEN products fetch fails WHEN searchForDomains THEN return Success with null sale prices`() =
        test {
            mockProductsErrorThenSuggestions(
                suggestions = listOf(
                    DomainSuggestion.Paid(
                        paidSuggestion(
                            domainName = "example.com",
                            cost = "\$50.00",
                            productId = 6u,
                        )
                    )
                )
            )

            val result = repository.searchForDomains("query")

            val success =
                result as NewDomainsSearchRepository.DomainsResult.Success
            assertThat(success.proposedDomains).hasSize(1)
            assertThat(success.proposedDomains[0].domain)
                .isEqualTo("example.com")
            assertThat(success.proposedDomains[0].price)
                .isEqualTo("\$50.00")
            assertThat(success.proposedDomains[0].salePrice).isNull()
        }

    @Test
    fun `GIVEN API error WHEN searchForDomains THEN return Error`() =
        test {
            mockProductsThenError()

            val result = repository.searchForDomains("query")

            assertThat(result)
                .isEqualTo(NewDomainsSearchRepository.DomainsResult.Error)
        }

    @Suppress("UNCHECKED_CAST")
    private suspend fun mockProductsThenSuggestions(
        products: Map<String, Product> = emptyMap(),
        suggestions: List<DomainSuggestion>,
    ) {
        whenever(wpComApiClient.request<Any>(any()))
            .thenReturn(
                WpRequestResult.Success(products) as WpRequestResult<Any>,
                WpRequestResult.Success(suggestions) as WpRequestResult<Any>,
            )
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun mockProductsErrorThenSuggestions(
        suggestions: List<DomainSuggestion>,
    ) {
        whenever(wpComApiClient.request<Any>(any()))
            .thenReturn(
                WpRequestResult.UnknownError<Any>(
                    500.toUInt(),
                    "Internal Server Error",
                    "",
                    RequestMethod.GET
                ),
                WpRequestResult.Success(suggestions)
                    as WpRequestResult<Any>,
            )
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun mockProductsThenError() {
        whenever(wpComApiClient.request<Any>(any()))
            .thenReturn(
                WpRequestResult.Success(emptyMap<String, Product>()) as WpRequestResult<Any>,
                WpRequestResult.UnknownError<Any>(
                    500.toUInt(),
                    "Internal Server Error",
                    "",
                    RequestMethod.GET
                ),
            )
    }

    private fun paidSuggestion(
        domainName: String = "example.com",
        relevance: Double = 0.0,
        cost: String = "\$18.00",
        productId: ULong = 6u,
        supportsPrivacy: Boolean = true,
    ) = PaidDomainSuggestion(
        domainName = domainName,
        relevance = relevance,
        supportsPrivacy = supportsPrivacy,
        vendor = "donuts",
        matchReasons = listOf("tld-common"),
        maxRegYears = 10u,
        multiYearRegAllowed = true,
        productId = productId,
        productSlug = "domain_reg",
        cost = cost,
        renewCost = cost,
        renewRawPrice = 1800L,
        rawPrice = 1800L,
        currencyCode = "USD",
        saleCost = null,
        hstsRequired = null,
        policyNotices = emptyList(),
    )

    private fun testProduct(
        productId: ULong = 6u,
        combinedSaleCostDisplay: String? = null,
    ) = Product(
        productId = productId,
        productName = "Domain Registration",
        productSlug = "domain_reg",
        description = "Register a domain",
        productType = "domains",
        available = true,
        billingProductSlug = "domain_reg",
        isDomainRegistration = true,
        costDisplay = "$18.00",
        combinedCostDisplay = "$18",
        cost = 1800L,
        costSmallestUnit = 1800u,
        currencyCode = "USD",
        productTerm = uniffi.wp_api.ProductTerm.Year,
        productTermLocalized = "year",
        priceTierSlug = "",
        priceTierList = emptyList(),
        domainInfo = null,
        costPerMonthDisplay = null,
        saleCost = null,
        combinedSaleCostDisplay = combinedSaleCostDisplay,
        saleCoupon = null,
        introductoryOffer = null,
    )
}
