package org.wordpress.android.ui.domains

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.TransactionsRestClient.CreateShoppingCartResponse
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.TransactionsStore.OnShoppingCartCreated
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.CTA_DOMAIN_CREDIT_REDEMPTION
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.DOMAIN_PURCHASE
import org.wordpress.android.ui.domains.usecases.CreateCartUseCase
import org.wordpress.android.util.helpers.Debouncer
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.DomainSuggestion
import uniffi.wp_api.FreeDomainSuggestion
import uniffi.wp_api.PaidDomainSuggestion
import uniffi.wp_api.Product
import uniffi.wp_api.ProductTerm
import uniffi.wp_api.RequestMethod
import uniffi.wp_api.WpErrorCode

@ExperimentalCoroutinesApi
class DomainSuggestionsViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var wpComApiClientProvider: WpComApiClientProvider

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var wpComApiClient: WpComApiClient

    @Mock
    lateinit var debouncer: Debouncer

    @Mock
    lateinit var tracker: DomainsRegistrationTracker

    @Mock
    lateinit var createCartUseCase: CreateCartUseCase

    private lateinit var site: SiteModel
    private lateinit var domainRegistrationPurpose: DomainRegistrationPurpose
    private lateinit var viewModel: DomainSuggestionsViewModel
    private lateinit var onDomainSelectedEvents: MutableList<DomainProductDetails>
    private lateinit var suggestionStates: MutableList<ListState<DomainSuggestionItem>>

    @Before
    fun setUp() {
        site = SiteModel().also { it.name = "Test Site" }
        domainRegistrationPurpose = CTA_DOMAIN_CREDIT_REDEMPTION

        whenever(accountStore.accessToken).thenReturn("test-token")
        whenever(wpComApiClientProvider.getWpComApiClient("test-token"))
            .thenReturn(wpComApiClient)

        whenever(debouncer.debounce(any(), any(), any(), any())).thenAnswer { invocation ->
            val delayedRunnable = invocation.arguments[1] as Runnable
            delayedRunnable.run()
        }
        // Every request has to resolve to something: the view model logs the
        // result, and an unstubbed mock hands back null for a non-null type.
        runBlocking { mockResponses(productsResponse(), suggestionsResponse()) }

        viewModel = createViewModel(testDispatcher())
    }

    private fun createViewModel(dispatcher: CoroutineDispatcher): DomainSuggestionsViewModel {
        val created = DomainSuggestionsViewModel(
            wpComApiClientProvider,
            accountStore,
            tracker,
            debouncer,
            createCartUseCase,
            dispatcher
        )

        onDomainSelectedEvents = mutableListOf()
        created.onDomainSelected.observeForever { onDomainSelectedEvents.add(it.peekContent()) }

        suggestionStates = mutableListOf()
        created.suggestionsLiveData.observeForever { suggestionStates.add(it) }

        return created
    }

    /**
     * The view model issues the products request first and the suggestions
     * request second, so responses are stubbed in that order.
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun mockResponses(vararg responses: WpRequestResult<*>) {
        whenever(wpComApiClient.request<Any>(any()))
            .thenReturn(
                responses.first() as WpRequestResult<Any>,
                *responses.drop(1).map { it as WpRequestResult<Any> }.toTypedArray()
            )
    }

    @Test
    fun `redirect message is visible when purchasing a domain at start`() {
        domainRegistrationPurpose = DOMAIN_PURCHASE
        viewModel.start(site, domainRegistrationPurpose)
        assertNotNull(viewModel.showRedirectMessage.value)
        viewModel.showRedirectMessage.value?.let { siteUrl ->
            assertNotNull(siteUrl)
        }
    }

    @Test
    fun `intro is visible at start`() {
        viewModel.start(site, domainRegistrationPurpose)
        assertNotNull(viewModel.isIntroVisible.value)
        viewModel.isIntroVisible.value?.let { isIntroVisible ->
            assert(isIntroVisible)
        }
    }

    @Test
    fun `intro is hidden when search query is not empty`() {
        viewModel.start(site, domainRegistrationPurpose)
        viewModel.updateSearchQuery("Hello World")

        assertNotNull(viewModel.isIntroVisible.value)
        viewModel.isIntroVisible.value?.let { isIntroVisible ->
            assertFalse(isIntroVisible)
        }
    }

    @Test
    fun `intro is visible when search query is empty`() {
        viewModel.start(site, domainRegistrationPurpose)
        viewModel.updateSearchQuery("Hello World")
        viewModel.updateSearchQuery("")

        assertNotNull(viewModel.isIntroVisible.value)
        viewModel.isIntroVisible.value?.let { isIntroVisible ->
            assert(isIntroVisible)
        }
    }

    @Test
    fun `domain products are fetched only at first start`() = test {
        mockResponses(productsResponse(), suggestionsResponse())

        viewModel.start(site, domainRegistrationPurpose)
        viewModel.start(site, domainRegistrationPurpose)
        advanceUntilIdle()

        // One products request and one suggestions request, not two of each.
        verify(wpComApiClient, times(2)).request<Any>(any())
    }

    @Test
    fun `site on blogger plan is requesting only dot blog domain suggestions`() {
        val params = viewModel.buildSuggestionsParams("test", isOnBloggerPlan = true)

        assertThat(params.query).isEqualTo("test")
        assertThat(params.tlds).isEqualTo(listOf("blog"))
        assertThat(params.onlyWordpressdotcom).isNull() // checkstyle ignore
        assertThat(params.includeWordpressdotcom).isNull() // checkstyle ignore
        assertThat(params.includeDotblogsubdomain).isNull()
        assertThat(params.vendor).isNull()
    }

    @Test
    fun `site on non blogger plan is requesting all possible domain suggestions`() {
        val params = viewModel.buildSuggestionsParams("test", isOnBloggerPlan = false)

        assertThat(params.query).isEqualTo("test")
        assertThat(params.onlyWordpressdotcom).isFalse() // checkstyle ignore
        assertThat(params.includeWordpressdotcom).isFalse() // checkstyle ignore
        assertThat(params.includeDotblogsubdomain).isTrue()
        assertThat(params.vendor).isNull()
        assertThat(params.tlds).isNull()
    }

    @Test
    fun `free suggestions are dropped and paid ones are sorted by relevance`() = test {
        mockResponses(
            productsResponse(),
            suggestionsResponse(
                DomainSuggestion.Free(
                    FreeDomainSuggestion("test.wordpress.com", "Free", isFree = true)
                ),
                DomainSuggestion.Paid(paidSuggestion("low.com", relevance = 0.2)),
                DomainSuggestion.Paid(paidSuggestion("high.com", relevance = 0.9)),
            )
        )

        viewModel.start(site, domainRegistrationPurpose)
        advanceUntilIdle()

        assertThat(lastSuccess().map { it.domainName })
            .containsExactly("high.com", "low.com")
    }

    @Test
    fun `a product on sale is mapped to its server formatted sale cost`() = test {
        mockResponses(
            productsResponse(
                "domain_reg" to testProduct(
                    productId = 6u,
                    saleCost = 600L,
                    combinedSaleCostDisplay = "$6"
                )
            ),
            suggestionsResponse(DomainSuggestion.Paid(paidSuggestion(productId = 6u)))
        )

        viewModel.start(site, domainRegistrationPurpose)
        advanceUntilIdle()

        val item = lastSuccess().single()
        assertThat(item.isOnSale).isTrue()
        assertThat(item.saleCost).isEqualTo("$6")
    }

    @Test
    fun `a product with a zero sale cost is not on sale`() = test {
        mockResponses(
            productsResponse(
                "domain_reg" to testProduct(productId = 6u, saleCost = 0L)
            ),
            suggestionsResponse(DomainSuggestion.Paid(paidSuggestion(productId = 6u)))
        )

        viewModel.start(site, domainRegistrationPurpose)
        advanceUntilIdle()

        assertThat(lastSuccess().single().isOnSale).isFalse()
    }

    @Test
    fun `empty_results is shown as an empty list rather than an error`() = test {
        mockResponses(
            productsResponse(),
            wpError("empty_results", "No available domains for that search.")
        )

        viewModel.start(site, domainRegistrationPurpose)
        advanceUntilIdle()

        assertThat(suggestionStates.last()).isInstanceOf(ListState.Success::class.java)
        assertThat(lastSuccess()).isEmpty()
    }

    @Test
    fun `an api error surfaces its message to the view`() = test {
        mockResponses(
            productsResponse(),
            wpError("invalid_query", "Domain searches must contain a word")
        )

        viewModel.start(site, domainRegistrationPurpose)
        advanceUntilIdle()

        val state = suggestionStates.last()
        assertThat(state).isInstanceOf(ListState.Error::class.java)
        assertThat((state as ListState.Error).errorMessage)
            .isEqualTo("Domain searches must contain a word")
    }

    @Test
    fun `a non api failure leaves the message empty so the view shows its own`() = test {
        mockResponses(
            productsResponse(),
            WpRequestResult.UnknownError<Any>(
                500.toUInt(),
                "Internal Server Error",
                "",
                RequestMethod.GET,
            )
        )

        viewModel.start(site, domainRegistrationPurpose)
        advanceUntilIdle()

        val state = suggestionStates.last()
        assertThat(state).isInstanceOf(ListState.Error::class.java)
        assertThat((state as ListState.Error).errorMessage).isNull()
    }

    /**
     * A response that arrives after the search has moved on must not replace
     * the list. The requests are queued on a [StandardTestDispatcher] so both
     * are in flight before either resolves, which an unconfined dispatcher
     * cannot reproduce. `start()` is skipped so the products request does not
     * reset the query to the site name partway through.
     */
    @Test
    fun `suggestions for a superseded query are discarded`() = test {
        mockResponses(
            suggestionsResponse(DomainSuggestion.Paid(paidSuggestion("stale.com"))),
            suggestionsResponse(DomainSuggestion.Paid(paidSuggestion("fresh.com"))),
        )
        viewModel = createViewModel(StandardTestDispatcher(testDispatcher().scheduler))
        viewModel.site = site
        viewModel.domainRegistrationPurpose = domainRegistrationPurpose

        viewModel.updateSearchQuery("stale")
        viewModel.updateSearchQuery("fresh")
        advanceUntilIdle()

        val emitted = suggestionStates.filterIsInstance<ListState.Success<DomainSuggestionItem>>()
        assertThat(emitted).hasSize(1)
        assertThat(emitted.single().data.map { it.domainName }).containsExactly("fresh.com")
    }

    @Test
    fun `clicking select domain button for credit redemption emits selected domain`() = test {
        mockResponses(productsResponse(), suggestionsResponse())

        viewModel.start(site, CTA_DOMAIN_CREDIT_REDEMPTION)
        viewModel.onDomainSuggestionSelected(dummySelectedDomainSuggestionItem)
        viewModel.onSelectDomainButtonClicked()

        verifyNoInteractions(createCartUseCase)

        assertThat(onDomainSelectedEvents.last()).isEqualTo(DomainProductDetails(DUMMY_PRODUCT_ID, DUMMY_DOMAIN_NAME))
    }

    @Test
    fun `clicking select domain button for purchase calls cart creation use case and emits selected domain`() = test {
        mockResponses(productsResponse(), suggestionsResponse())
        whenever(createCartUseCase.execute(site, DUMMY_PRODUCT_ID, DUMMY_DOMAIN_NAME, true, false))
            .thenReturn(dummySuccessfulOnShoppingCartCreated)

        viewModel.start(site, DOMAIN_PURCHASE)
        viewModel.onDomainSuggestionSelected(dummySelectedDomainSuggestionItem)
        viewModel.onSelectDomainButtonClicked()

        assertThat(onDomainSelectedEvents.last()).isEqualTo(DomainProductDetails(DUMMY_PRODUCT_ID, DUMMY_DOMAIN_NAME))
    }

    private fun lastSuccess(): List<DomainSuggestionItem> =
        suggestionStates.filterIsInstance<ListState.Success<DomainSuggestionItem>>().last().data

    companion object {
        const val DUMMY_PRODUCT_ID = 1
        const val DUMMY_DOMAIN_NAME = "domainname.com"

        val dummySuccessfulOnShoppingCartCreated = OnShoppingCartCreated(
            CreateShoppingCartResponse(
                1,
                "dummy_cart_key",
                emptyList()
            )
        )

        val dummySelectedDomainSuggestionItem = DomainSuggestionItem(
            domainName = DUMMY_DOMAIN_NAME,
            cost = "$20.00",
            isOnSale = false,
            saleCost = "0.0",
            isFree = false,
            supportsPrivacy = true,
            productId = DUMMY_PRODUCT_ID,
            productSlug = null,
            vendor = null,
            relevance = 1.0f,
            isSelected = true,
            isCostVisible = true,
            isFreeWithCredits = false,
            isEnabled = true
        )

        private fun productsResponse(vararg products: Pair<String, Product>) =
            WpRequestResult.Success(products.toMap())

        private fun suggestionsResponse(vararg suggestions: DomainSuggestion) =
            WpRequestResult.Success(suggestions.toList())

        private fun wpError(code: String, message: String) = WpRequestResult.WpError<Any>(
            errorCode = WpErrorCode.CustomException(code),
            errorMessage = message,
            statusCode = 400.toUInt(),
            response = "",
            requestUrl = "",
            requestMethod = RequestMethod.GET,
        )

        private fun paidSuggestion(
            domainName: String = "example.com",
            relevance: Double = 0.0,
            productId: ULong = 6u,
        ) = PaidDomainSuggestion(
            domainName = domainName,
            relevance = relevance,
            supportsPrivacy = true,
            vendor = "donuts",
            matchReasons = listOf("tld-common"),
            maxRegYears = 10u,
            multiYearRegAllowed = true,
            productId = productId,
            productSlug = "domain_reg",
            cost = "\$18.00",
            renewCost = "\$18.00",
            renewRawPrice = 1800L,
            rawPrice = 1800L,
            currencyCode = "USD",
            saleCost = null,
            hstsRequired = null,
            policyNotices = emptyList(),
        )

        private fun testProduct(
            productId: ULong = 6u,
            saleCost: Long? = null,
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
            productTerm = ProductTerm.Year,
            productTermLocalized = "year",
            priceTierSlug = "",
            priceTierList = emptyList(),
            domainInfo = null,
            costPerMonthDisplay = null,
            saleCost = saleCost,
            combinedSaleCostDisplay = combinedSaleCostDisplay,
            saleCoupon = null,
            introductoryOffer = null,
        )
    }
}
