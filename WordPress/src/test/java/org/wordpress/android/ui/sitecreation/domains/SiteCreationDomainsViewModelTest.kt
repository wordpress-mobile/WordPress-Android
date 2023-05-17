package org.wordpress.android.ui.sitecreation.domains

import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.lastValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.secondValue
import org.mockito.kotlin.thirdValue
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.Constants.TYPE_DOMAINS_PRODUCT
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.products.Product
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse
import org.wordpress.android.fluxc.store.ProductsStore
import org.wordpress.android.fluxc.store.ProductsStore.OnProductsFetched
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainError
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.DomainsUiState
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.DomainsUiState.CreateSiteButtonState
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.DomainsUiState.DomainsUiContentState
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState.Cost
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState.Tag
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.Old
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.Old.DomainUiState.UnavailableDomain
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.usecases.FETCH_DOMAINS_VENDOR_DOT
import org.wordpress.android.ui.sitecreation.usecases.FETCH_DOMAINS_VENDOR_MOBILE
import org.wordpress.android.ui.sitecreation.usecases.FetchDomainsUseCase
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.config.SiteCreationDomainPurchasingFeatureConfig
import kotlin.test.assertIs

private const val MULTI_RESULT_DOMAIN_FETCH_RESULT_SIZE = 20
private val MULTI_RESULT_DOMAIN_FETCH_QUERY = "multi_result_query" to MULTI_RESULT_DOMAIN_FETCH_RESULT_SIZE
private val EMPTY_RESULT_DOMAIN_FETCH_QUERY = "empty_result_query" to 0
private val SALE_PRODUCTS = listOf(Product(productId = 3, saleCost = 1.0, currencyCode = "EUR"))

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteCreationDomainsViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var dispatcher: Dispatcher

    @Mock
    lateinit var fetchDomainsUseCase: FetchDomainsUseCase

    @Mock
    private lateinit var productsStore: ProductsStore

    @Mock
    lateinit var purchasingFeatureConfig: SiteCreationDomainPurchasingFeatureConfig

    @Mock
    private lateinit var tracker: SiteCreationTracker

    @Mock
    private lateinit var uiStateObserver: Observer<DomainsUiState>

    @Mock
    private lateinit var createSiteBtnObserver: Observer<DomainModel>

    @Mock
    private lateinit var clearBtnObserver: Observer<Unit>

    @Mock
    private lateinit var onHelpClickedObserver: Observer<Unit>

    @Mock
    private lateinit var networkUtils: NetworkUtilsWrapper

    @Mock
    private lateinit var mSiteCreationDomainSanitizer: SiteCreationDomainSanitizer

    private lateinit var viewModel: SiteCreationDomainsViewModel

    @Before
    fun setUp() {
        viewModel = SiteCreationDomainsViewModel(
            networkUtils = networkUtils,
            domainSanitizer = mSiteCreationDomainSanitizer,
            dispatcher = dispatcher,
            fetchDomainsUseCase = fetchDomainsUseCase,
            productsStore = productsStore,
            purchasingFeatureConfig = purchasingFeatureConfig,
            tracker = tracker,
            bgDispatcher = testDispatcher(),
            mainDispatcher = testDispatcher()
        )
        viewModel.uiState.observeForever(uiStateObserver)
        viewModel.createSiteBtnClicked.observeForever(createSiteBtnObserver)
        viewModel.clearBtnClicked.observeForever(clearBtnObserver)
        viewModel.onHelpClicked.observeForever(onHelpClickedObserver)
        whenever(networkUtils.isNetworkAvailable()).thenReturn(true)
    }

    private fun <T> testWithSuccessResponse(
        queryResultSizePair: Pair<String, Int> = MULTI_RESULT_DOMAIN_FETCH_QUERY,
        isDomainAvailableInSuggestions: Boolean = true,
        block: suspend CoroutineScope.() -> T
    ) {
        test {
            whenever(mSiteCreationDomainSanitizer.getName(any())).thenReturn(
                "${MULTI_RESULT_DOMAIN_FETCH_QUERY.first}-1"
            )
            whenever(mSiteCreationDomainSanitizer.getDomain(any())).thenReturn(
                ".wordpress.com"
            )
            whenever(mSiteCreationDomainSanitizer.sanitizeDomainQuery(any())).thenReturn(
                createSanitizedDomainResult(isDomainAvailableInSuggestions)
            )
            whenever(fetchDomainsUseCase.fetchDomains(eq(queryResultSizePair.first), any(), any(), any())).thenReturn(
                createSuccessfulOnSuggestedDomains(queryResultSizePair)
            )
            block()
        }
    }

    /**
     * Verifies the UI state for when the VM is started with an empty site title.
     */
    @Test
    fun verifyEmptyTitleQueryUiState() = testWithSuccessResponse {
        viewModel.start()
        verifyInitialContentUiState(requireNotNull(viewModel.uiState.value), showProgress = false)
    }

    /**
     * Verifies the initial UI state for when the user enters a non-empty query.
     */
    @Test
    fun verifyNonEmptyUpdateQueryInitialUiState() = testWithSuccessResponse {
        viewModel.start()
        viewModel.onQueryChanged(MULTI_RESULT_DOMAIN_FETCH_QUERY.first)
        val captor = ArgumentCaptor.forClass(DomainsUiState::class.java)
        advanceUntilIdle()

        verify(uiStateObserver, times(3)).onChanged(captor.capture())
        verifyInitialContentUiState(captor.secondValue, showProgress = true, showClearButton = true)
    }

    /**
     * Verifies the UI state for after the user enters a non-empty query which results in no domain suggestions.
     */
    @Test
    fun verifyNonEmptyUpdateQueryUiStateAfterResponseWithEmptyResults() = testWithSuccessResponse(
        queryResultSizePair = EMPTY_RESULT_DOMAIN_FETCH_QUERY
    ) {
        viewModel.start()
        viewModel.onQueryChanged(EMPTY_RESULT_DOMAIN_FETCH_QUERY.first)
        val captor = ArgumentCaptor.forClass(DomainsUiState::class.java)
        advanceUntilIdle()

        verify(uiStateObserver, times(3)).onChanged(captor.capture())
        verifyEmptyItemsContentUiState(
            captor.thirdValue,
            showClearButton = true
        )
    }

    /**
     * Verifies the UI state for after the user enters a non-empty query which results in multiple domain suggestions.
     */
    @Test
    fun verifyNonEmptyUpdateQueryUiStateAfterResponseWithMultipleResults() = testWithSuccessResponse {
        viewModel.start()
        viewModel.onQueryChanged(MULTI_RESULT_DOMAIN_FETCH_QUERY.first)
        val captor = ArgumentCaptor.forClass(DomainsUiState::class.java)
        advanceUntilIdle()

        verify(uiStateObserver, times(3)).onChanged(captor.capture())
        verifyVisibleItemsContentUiState(captor.thirdValue, showClearButton = true)
    }

    /**
     * Verifies the UI state for after the user enters a query that is unavailable which results in the domain
     * unavailability list item being shown in the domain suggestions.
     */
    @Test
    fun verifyDomainUnavailableUiStateAfterResponseWithMultipleResults() = testWithSuccessResponse(
        isDomainAvailableInSuggestions = false
    ) {
        viewModel.start()
        viewModel.onQueryChanged(MULTI_RESULT_DOMAIN_FETCH_QUERY.first)
        val captor = ArgumentCaptor.forClass(DomainsUiState::class.java)
        advanceUntilIdle()

        verify(uiStateObserver, times(3)).onChanged(captor.capture())
        verifyContentAndDomainValidityUiStatesAreVisible(
            captor.thirdValue
        )
    }

    /**
     * Verifies the UI state for after the user enters a non-empty query which results in error.
     */
    @Test
    fun verifyNonEmptyUpdateQueryUiStateAfterErrorResponse() = test {
        val queryResultErrorPair = "error_result_query" to "GENERIC_ERROR"
        whenever(fetchDomainsUseCase.fetchDomains(eq(queryResultErrorPair.first), any(), any(), any())).thenReturn(
            createFailedOnSuggestedDomains(queryResultErrorPair)
        )

        viewModel.start()
        viewModel.onQueryChanged(queryResultErrorPair.first)
        advanceUntilIdle()

        val captor = ArgumentCaptor.forClass(DomainsUiState::class.java)
        verify(uiStateObserver, times(3)).onChanged(captor.capture())
        verifyVisibleItemsContentUiState(
            captor.thirdValue,
            showClearButton = true,
            numberOfItems = 1
        )
        assertThat(captor.thirdValue.contentState.items[0])
            .isInstanceOf(Old.ErrorItemUiState::class.java)
    }

    /**
     * Verifies the UI state for after the user enters a non-empty query which results in INVALID_QUERY error.
     */
    @Test
    fun verifyNonEmptyUpdateQueryUiStateAfterErrorResponseOfTypeInvalidQuery() = test {
        val queryResultErrorPair = "empty_result_query_invalid" to "INVALID_QUERY"
        whenever(fetchDomainsUseCase.fetchDomains(eq(queryResultErrorPair.first), any(), any(), any())).thenReturn(
            createFailedOnSuggestedDomains(queryResultErrorPair)
        )

        viewModel.start()
        viewModel.onQueryChanged(queryResultErrorPair.first)
        advanceUntilIdle()

        val captor = ArgumentCaptor.forClass(DomainsUiState::class.java)
        verify(uiStateObserver, times(3)).onChanged(captor.capture())
        verifyEmptyItemsContentUiState(
            captor.thirdValue,
            showClearButton = true,
            isInvalidQuery = true
        )
    }

    /**
     * Verifies the UI state after the user enters an empty query (presses clear button) with an empty site title
     * which results in initial UI state
     */
    @Test
    fun verifyClearQueryWithEmptyTitleInitialState() = testWithSuccessResponse {
        viewModel.start()
        viewModel.onQueryChanged(MULTI_RESULT_DOMAIN_FETCH_QUERY.first)
        viewModel.onQueryChanged("")
        val captor = ArgumentCaptor.forClass(DomainsUiState::class.java)
        advanceUntilIdle()

        verify(uiStateObserver, times(3)).onChanged(captor.capture())
        verifyInitialContentUiState(captor.lastValue)
    }

    /**
     * Verifies that help button is properly propagated.
     */
    @Test
    fun verifyOnHelpClickedPropagated() = testWithSuccessResponse {
        viewModel.onHelpClicked()
        val captor = ArgumentCaptor.forClass(Unit::class.java)
        verify(onHelpClickedObserver, times(1)).onChanged(captor.capture())
    }

    /**
     * Verifies that clear button is properly propagated.
     */
    @Test
    fun verifyOnClearBtnClickedPropagated() = testWithSuccessResponse {
        viewModel.onClearTextBtnClicked()
        val captor = ArgumentCaptor.forClass(Unit::class.java)
        verify(clearBtnObserver, times(1)).onChanged(captor.capture())
    }

    @Test
    fun `verify click on the create site button emits the selected domain`() = testWithSuccessResponse {
        val selectedDomain = mockDomain("test.domain")
        viewModel.onDomainSelected(selectedDomain)

        viewModel.onCreateSiteBtnClicked()

        verify(createSiteBtnObserver).onChanged(argWhere { it == selectedDomain })
    }

    @Test
    fun `click on the create site button tracks the selected free domain`() = testWithSuccessResponse {
        val selectedDomain = mockDomain("test.domain")
        val expectedCost = "Free"
        whenever(selectedDomain.cost).thenReturn(expectedCost)
        viewModel.onDomainSelected(selectedDomain)

        viewModel.onCreateSiteBtnClicked()

        verify(tracker).trackDomainSelected(selectedDomain.domainName, "", expectedCost)
    }

    @Test
    fun `click on the create site button tracks the selected paid domain`() = testWithSuccessResponse {
        val selectedDomain = mockDomain("test.domain", free = false)
        val expectedCost = "1.23 USD"
        whenever(selectedDomain.cost).thenReturn(expectedCost)
        viewModel.onDomainSelected(selectedDomain)

        viewModel.onCreateSiteBtnClicked()

        verify(tracker).trackDomainSelected(selectedDomain.domainName, "", expectedCost)
    }

    @Test
    fun verifyFetchFreeDomainsWhenPurchasingFeatureConfigIsDisabled() = testWithSuccessResponse {
        viewModel.start()

        viewModel.onQueryChanged(MULTI_RESULT_DOMAIN_FETCH_QUERY.first)
        advanceUntilIdle()

        verify(fetchDomainsUseCase).fetchDomains(
            eq(MULTI_RESULT_DOMAIN_FETCH_QUERY.first),
            eq(FETCH_DOMAINS_VENDOR_DOT),
            eq(true),
            eq(MULTI_RESULT_DOMAIN_FETCH_QUERY.second),
        )
    }

    @Test
    fun `verify create site button text is not changed when purchasing feature is OFF`() = testWithSuccessResponse {
        viewModel.start()

        viewModel.onDomainSelected(mock())

        assertIs<CreateSiteButtonState.Old>(viewModel.uiState.value?.createSiteButtonState)
    }

    // region New UI

    private fun testNewUi(block: suspend CoroutineScope.() -> Unit) = test {
        whenever(purchasingFeatureConfig.isEnabledOrManuallyOverridden()).thenReturn(true)
        whenever(productsStore.fetchProducts(any())).thenReturn(mock())
        block()
    }

    private fun <T> testWithSuccessResultNewUi(
        queryToSize: Pair<String, Int> = MULTI_RESULT_DOMAIN_FETCH_QUERY,
        block: suspend CoroutineScope.(OnSuggestedDomains) -> T
    ) = testNewUi {
        val (query, size) = queryToSize

        val suggestions = List(size) {
            DomainSuggestionResponse().apply {
                domain_name = if (it == 0) query else "$query-$it.com"
                is_free = it % 2 == 0
                cost = if (is_free) "Free" else "$$it.00"
                product_id = it
                supports_privacy = !is_free
            }
        }

        val event = OnSuggestedDomains(query, suggestions)
        whenever(fetchDomainsUseCase.fetchDomains(any(), any(), any(), any())).thenReturn(event)
        block(event)
    }

    private val uiDomains get() = assertIs<List<New.DomainUiState>>(viewModel.uiState.value?.contentState?.items)

    @Test
    fun verifyFetchFreeAndPaidDomainsWhenPurchasingFeatureConfigIsEnabled() = testNewUi {
        val (query, size) = MULTI_RESULT_DOMAIN_FETCH_QUERY
        viewModel.start()

        viewModel.onQueryChanged(query)
        advanceUntilIdle()

        verify(fetchDomainsUseCase).fetchDomains(
            eq(query),
            eq(FETCH_DOMAINS_VENDOR_MOBILE),
            eq(false),
            eq(size),
        )
    }

    @Test
    fun `verify domain products are fetched only at first start`() = testNewUi {
        viewModel.start()
        viewModel.start()

        verify(productsStore).fetchProducts(eq(TYPE_DOMAINS_PRODUCT))
    }

    @Test
    fun `verify create site button text changes when selecting a free domain`() = testNewUi {
        viewModel.start()

        viewModel.onDomainSelected(mockDomain(free = true))

        assertIs<CreateSiteButtonState.Free>(viewModel.uiState.value?.createSiteButtonState)
    }

    @Test
    fun `verify create site button text changes when selecting a non-free domain`() = testNewUi {
        viewModel.start()

        viewModel.onDomainSelected(mockDomain(free = false))

        assertIs<CreateSiteButtonState.Paid>(viewModel.uiState.value?.createSiteButtonState)
    }


    @Test
    fun `verify all domain results from api are visible`() = testWithSuccessResultNewUi { (query, results) ->
        viewModel.start()

        viewModel.onQueryChanged(query)
        advanceUntilIdle()

        assertThat(uiDomains).hasSameSizeAs(results)
    }

    @Test
    fun `verify cost of free domain results from api is 'Free'`() = testWithSuccessResultNewUi { (query, results) ->
        val apiFreeDomains = results.filter { it.is_free }
        viewModel.start()

        viewModel.onQueryChanged(query)
        advanceUntilIdle()

        assertThat(uiDomains).filteredOn { it.cost is Cost.Free }.hasSameSizeAs(apiFreeDomains)
    }

    @Test
    fun `verify cost of paid domain results from api is 'Paid'`() = testWithSuccessResultNewUi { (query, results) ->
        val apiPaidDomains = results.filter { !it.is_free }
        viewModel.start()

        viewModel.onQueryChanged(query)
        advanceUntilIdle()

        assertThat(uiDomains).filteredOn { it.cost is Cost.Paid }.hasSameSizeAs(apiPaidDomains)
    }

    @Test
    fun `verify cost of sale domain results from api is 'OnSale'`() = testWithSuccessResultNewUi { (query) ->
        whenever(productsStore.fetchProducts(any())).thenReturn(OnProductsFetched(SALE_PRODUCTS))

        viewModel.start()

        viewModel.onQueryChanged(query)
        advanceUntilIdle()

        assertThat(uiDomains).filteredOn { it.cost is Cost.OnSale }.hasSameSizeAs(SALE_PRODUCTS)
    }

    @Test
    fun `verify sale domain results from api have tag 'Sale'`() = testWithSuccessResultNewUi { (query) ->
        whenever(productsStore.fetchProducts(any())).thenReturn(OnProductsFetched(SALE_PRODUCTS))

        viewModel.start()

        viewModel.onQueryChanged(query)
        advanceUntilIdle()

        assertThat(uiDomains.flatMap { it.tags }).filteredOn { it is Tag.Sale }.hasSameSizeAs(SALE_PRODUCTS)
    }

    @Test
    fun `verify only 1st domain result from api is 'Recommended'`() = testWithSuccessResultNewUi { (query) ->
        viewModel.start()

        viewModel.onQueryChanged(query)
        advanceUntilIdle()

        assertThat(uiDomains.flatMap { it.tags }).filteredOn { it is Tag.Recommended }.singleElement()
    }

    @Test
    fun `verify only 2nd domain result from api is 'BestAlternative'`() = testWithSuccessResultNewUi { (query) ->
        viewModel.start()

        viewModel.onQueryChanged(query)
        advanceUntilIdle()

        assertThat(uiDomains.flatMap { it.tags }).filteredOn { it is Tag.BestAlternative }.singleElement()
    }

    @Test
    fun `verify selected domain is propagated to UI on click`() = testWithSuccessResultNewUi { (query) ->
        viewModel.start()

        viewModel.onQueryChanged(query)
        advanceUntilIdle()
        viewModel.onDomainSelected(mockDomain(query))

        assertThat(uiDomains.map { it.isSelected }).containsOnlyOnce(true)
    }

    // endregion

    // region Helpers
    /**
     * Helper function to verify a [DomainsUiState] with [DomainsUiContentState.Initial] content state.
     */
    private fun verifyInitialContentUiState(
        uiState: DomainsUiState,
        showProgress: Boolean = false,
        showClearButton: Boolean = false
    ) {
        assertThat(uiState.searchInputUiState.showProgress).isEqualTo(showProgress)
        assertThat(uiState.searchInputUiState.showClearButton).isEqualTo(showClearButton)
        assertThat(uiState.contentState).isInstanceOf(DomainsUiContentState.Initial::class.java)
        assertThat(uiState.createSiteButtonState).isNull()
    }

    /**
     * Helper function to verify a [DomainsUiState] with [DomainsUiContentState.VisibleItems] content state.
     */
    private fun verifyVisibleItemsContentUiState(
        uiState: DomainsUiState,
        showClearButton: Boolean = false,
        numberOfItems: Int = MULTI_RESULT_DOMAIN_FETCH_RESULT_SIZE
    ) {
        assertThat(uiState.searchInputUiState.showProgress).isEqualTo(false)
        assertThat(uiState.searchInputUiState.showClearButton).isEqualTo(showClearButton)
        assertThat(uiState.contentState).isInstanceOf(DomainsUiContentState.VisibleItems::class.java)
        assertThat(uiState.contentState.items.size).isEqualTo(numberOfItems)
    }

    /**
     * Helper function to verify a [UnavailableDomain] Ui State.
     */
    private fun verifyContentAndDomainValidityUiStatesAreVisible(
        uiState: DomainsUiState
    ) {
        assertThat(uiState.contentState.items.first())
            .isInstanceOf(UnavailableDomain::class.java)
    }

    /**
     * Helper function to verify a [DomainsUiState] with [DomainsUiContentState.Empty] content state.
     */
    private fun verifyEmptyItemsContentUiState(
        uiState: DomainsUiState,
        showClearButton: Boolean = false,
        isInvalidQuery: Boolean = false
    ) {
        assertThat(uiState.searchInputUiState.showProgress).isEqualTo(false)
        assertThat(uiState.searchInputUiState.showClearButton).isEqualTo(showClearButton)
        assertThat(uiState.contentState).isInstanceOf(DomainsUiContentState.Empty::class.java)
        val contentStateAsEmpty = uiState.contentState as DomainsUiContentState.Empty
        assertThat(contentStateAsEmpty.message).isInstanceOf(UiStringRes::class.java)
        val expectedEmptyListTextMessage = if (isInvalidQuery) {
            R.string.new_site_creation_empty_domain_list_message_invalid_query
        } else {
            R.string.new_site_creation_empty_domain_list_message
        }
        assertThat((contentStateAsEmpty.message as UiStringRes).stringRes).isEqualTo(expectedEmptyListTextMessage)
        assertThat(uiState.contentState.items.size).isEqualTo(0)
    }

    /**
     * Helper function that creates an [OnSuggestedDomains] event for the given query and number of results pair.
     */
    private fun createSuccessfulOnSuggestedDomains(queryResultSizePair: Pair<String, Int>): OnSuggestedDomains {
        val suggestions = (0 until queryResultSizePair.second).map {
            val response = DomainSuggestionResponse()
            response.domain_name = "${queryResultSizePair.first}-$it.wordpress.com"
            response
        }
        return OnSuggestedDomains(queryResultSizePair.first, suggestions)
    }

    /**
     * Helper function that creates an error [OnSuggestedDomains] event.
     */
    private fun createFailedOnSuggestedDomains(queryResultErrorPair: Pair<String, String>): OnSuggestedDomains {
        return OnSuggestedDomains(queryResultErrorPair.first, emptyList())
            .apply {
                error = SuggestDomainError(queryResultErrorPair.second, "test")
            }
    }

    /**
     * Helper function that creates the current sanitized query being used to generate the domain suggestions.
     * It returns a test domain that's based on the test suggestions being used so that the app can behave in it's
     * normal [Old.DomainUiState.AvailableDomain] state. It also returns an unavailable domain query so that the
     *  [Old.DomainUiState.UnavailableDomain] state is activated.
     */
    private fun createSanitizedDomainResult(isDomainAvailableInSuggestions: Boolean) =
        if (isDomainAvailableInSuggestions) {
            "${MULTI_RESULT_DOMAIN_FETCH_QUERY.first}-1"
        } else {
            "invaliddomain"
        }

    private fun mockDomain(name: String = "", free: Boolean = true) = mock<DomainModel> {
        on { domainName } doReturn name
        on { isFree } doReturn free
    }
    // endregion
}
