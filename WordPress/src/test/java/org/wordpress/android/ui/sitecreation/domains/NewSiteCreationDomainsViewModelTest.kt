package org.wordpress.android.ui.sitecreation.domains

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import com.nhaarman.mockitokotlin2.firstValue
import com.nhaarman.mockitokotlin2.secondValue
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainError
import org.wordpress.android.test
import org.wordpress.android.ui.sitecreation.domains.NewSiteCreationDomainsViewModel.DomainsListItemUiState.DomainsFetchSuggestionsErrorUiState
import org.wordpress.android.ui.sitecreation.domains.NewSiteCreationDomainsViewModel.DomainsUiState
import org.wordpress.android.ui.sitecreation.domains.NewSiteCreationDomainsViewModel.DomainsUiState.DomainsUiContentState
import org.wordpress.android.ui.sitecreation.misc.NewSiteCreationTracker
import org.wordpress.android.ui.sitecreation.usecases.FetchDomainsUseCase
import org.wordpress.android.util.NetworkUtilsWrapper
import org.hamcrest.CoreMatchers.`is` as Is

private const val MULTI_RESULT_DOMAIN_FETCH_RESULT_SIZE = 20
private const val ERROR_RESULT_FETCH_QUERY = "error_result_query"
private val MULTI_RESULT_DOMAIN_FETCH_QUERY = Pair("multi_result_query", MULTI_RESULT_DOMAIN_FETCH_RESULT_SIZE)
private val EMPTY_RESULT_DOMAIN_FETCH_QUERY = Pair("empty_result_query", 0)

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class NewSiteCreationDomainsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var fetchDomainsUseCase: FetchDomainsUseCase
    @Mock private lateinit var tracker: NewSiteCreationTracker
    @Mock private lateinit var uiStateObserver: Observer<DomainsUiState>
    @Mock private lateinit var createSiteBtnObserver: Observer<String>
    @Mock private lateinit var clearBtnObserver: Observer<Unit>
    @Mock private lateinit var onHelpClickedObserver: Observer<Unit>
    @Mock private lateinit var networkUtils: NetworkUtilsWrapper

    private lateinit var viewModel: NewSiteCreationDomainsViewModel

    @Before
    fun setUp() {
        viewModel = NewSiteCreationDomainsViewModel(
                networkUtils = networkUtils,
                dispatcher = dispatcher,
                fetchDomainsUseCase = fetchDomainsUseCase,
                tracker = tracker,
                bgDispatcher = TEST_DISPATCHER,
                mainDispatcher = TEST_DISPATCHER
        )
        viewModel.uiState.observeForever(uiStateObserver)
        viewModel.createSiteBtnClicked.observeForever(createSiteBtnObserver)
        viewModel.clearBtnClicked.observeForever(clearBtnObserver)
        viewModel.onHelpClicked.observeForever(onHelpClickedObserver)
        whenever(networkUtils.isNetworkAvailable()).thenReturn(true)
    }

    private fun <T> testWithSuccessResponse(
        queryResultSizePair: Pair<String, Int> = MULTI_RESULT_DOMAIN_FETCH_QUERY,
        block: suspend CoroutineScope.() -> T
    ) {
        test {
            whenever(fetchDomainsUseCase.fetchDomains(queryResultSizePair.first))
                    .thenReturn(createSuccessfulOnSuggestedDomains(queryResultSizePair))
            block()
        }
    }

    private fun <T> testWithErrorResponse(
        block: suspend CoroutineScope.() -> T
    ) {
        test {
            whenever(fetchDomainsUseCase.fetchDomains(ERROR_RESULT_FETCH_QUERY))
                    .thenReturn(createFailedOnSuggestedDomains(ERROR_RESULT_FETCH_QUERY))
            block()
        }
    }

    /**
     * Verifies the UI state for when the VM is started with an empty site title.
     */
    @Test
    fun verifyEmptyTitleQueryUiState() = testWithSuccessResponse {
        viewModel.start(null)
        verifyInitialContentUiState(requireNotNull(viewModel.uiState.value), showProgress = false)
    }

    /**
     * Verifies the initial UI state for when the VM is started with a non-empty site title.
     */
    @Test
    fun verifyMultiResultTitleQueryInitialUiState() = testWithSuccessResponse {
        viewModel.start(MULTI_RESULT_DOMAIN_FETCH_QUERY.first)
        val captor = ArgumentCaptor.forClass(DomainsUiState::class.java)
        verify(uiStateObserver, times(2)).onChanged(captor.capture())
        verifyInitialContentUiState(requireNotNull(captor.firstValue), showProgress = true)
    }

    /**
     * Verifies the UI state for after the VM is started with a non-empty site title and it results in some domain
     * suggestions.
     */
    @Test
    fun verifyMultiResultTitleQueryUiStateAfterResponse() = testWithSuccessResponse {
        viewModel.start(MULTI_RESULT_DOMAIN_FETCH_QUERY.first)
        val captor = ArgumentCaptor.forClass(DomainsUiState::class.java)
        verify(uiStateObserver, times(2)).onChanged(captor.capture())
        verifyVisibleItemsContentUiState(captor.secondValue)
    }

    /**
     * Verifies the UI state for after the VM is started with a non-empty site title and fetchDomains results in error
     */
    @Test
    fun verifyErrorResultTitleQueryUiStateAfterResponse() = testWithErrorResponse {
        viewModel.start(ERROR_RESULT_FETCH_QUERY)
        val captor = ArgumentCaptor.forClass(DomainsUiState::class.java)
        verify(uiStateObserver, times(2)).onChanged(captor.capture())
        verifyInitialContentUiState(captor.secondValue)
    }

    /**
     * Verifies the initial UI state for when the user enters a non-empty query.
     */
    @Test
    fun verifyNonEmptyUpdateQueryInitialUiState() = testWithSuccessResponse {
        viewModel.updateQuery(MULTI_RESULT_DOMAIN_FETCH_QUERY.first)
        val captor = ArgumentCaptor.forClass(DomainsUiState::class.java)
        verify(uiStateObserver, times(2)).onChanged(captor.capture())
        verifyInitialContentUiState(captor.firstValue, showProgress = true, showClearButton = true)
    }

    /**
     * Verifies the UI state for after the user enters a non-empty query which results in no domain suggestions.
     */
    @Test
    fun verifyNonEmptyUpdateQueryUiStateAfterResponseWithEmptyResults() =
            testWithSuccessResponse(queryResultSizePair = EMPTY_RESULT_DOMAIN_FETCH_QUERY) {
                viewModel.updateQuery(EMPTY_RESULT_DOMAIN_FETCH_QUERY.first)
                val captor = ArgumentCaptor.forClass(DomainsUiState::class.java)
                verify(uiStateObserver, times(2)).onChanged(captor.capture())
                verifyEmptyItemsContentUiState(
                        captor.secondValue,
                        showClearButton = true
                )
            }

    /**
     * Verifies the UI state for after the user enters a non-empty query which results in multiple domain suggestions.
     */
    @Test
    fun verifyNonEmptyUpdateQueryUiStateAfterResponseWithMultipleResults() = testWithSuccessResponse {
        viewModel.updateQuery(MULTI_RESULT_DOMAIN_FETCH_QUERY.first)
        val captor = ArgumentCaptor.forClass(DomainsUiState::class.java)
        verify(uiStateObserver, times(2)).onChanged(captor.capture())
        verifyVisibleItemsContentUiState(captor.secondValue, showClearButton = true)
    }

    /**
     * Verifies the UI state for after the user enters a non-empty query which results in error.
     */
    @Test
    fun verifyNonEmptyUpdateQueryUiStateAfterErrorResponse() = testWithErrorResponse {
        viewModel.updateQuery(ERROR_RESULT_FETCH_QUERY)
        val captor = ArgumentCaptor.forClass(DomainsUiState::class.java)
        verify(uiStateObserver, times(2)).onChanged(captor.capture())
        verifyVisibleItemsContentUiState(captor.secondValue, showClearButton = true, numberOfItems = 1)
        assertThat(
                captor.secondValue.contentState.items[0],
                instanceOf(DomainsFetchSuggestionsErrorUiState::class.java)
        )
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

    /**
     * Verifies that create site button is properly propagated when a domain is selected.
     */
    @Test
    fun verifyCreateSiteBtnClickedPropagated() = testWithSuccessResponse {
        val domainName = "test.domain"
        viewModel.setSelectedDomainName(domainName)
        viewModel.createSiteBtnClicked()
        val captor = ArgumentCaptor.forClass(String::class.java)
        verify(createSiteBtnObserver, times(1)).onChanged(captor.capture())
        assertThat(captor.firstValue, Is(domainName))
    }

    /**
     * Helper function to verify a [DomainsUiState] with [DomainsUiContentState.Initial] content state.
     */
    private fun verifyInitialContentUiState(
        uiState: DomainsUiState,
        showProgress: Boolean = false,
        showClearButton: Boolean = false
    ) {
        assertThat(uiState.searchInputUiState.showProgress, Is(showProgress))
        assertThat(uiState.searchInputUiState.showClearButton, Is(showClearButton))
        assertThat(uiState.contentState, instanceOf(DomainsUiContentState.Initial::class.java))
        assertThat(uiState.createSiteButtonContainerVisibility, Is(false))
    }

    /**
     * Helper function to verify a [DomainsUiState] with [DomainsUiContentState.VisibleItems] content state.
     */
    private fun verifyVisibleItemsContentUiState(
        uiState: DomainsUiState,
        showClearButton: Boolean = false,
        numberOfItems: Int = MULTI_RESULT_DOMAIN_FETCH_RESULT_SIZE
    ) {
        assertThat(uiState.searchInputUiState.showProgress, Is(false))
        assertThat(uiState.searchInputUiState.showClearButton, Is(showClearButton))
        assertThat(uiState.contentState, instanceOf(DomainsUiContentState.VisibleItems::class.java))
        assertThat(uiState.contentState.items.size, Is(numberOfItems))
    }

    /**
     * Helper function to verify a [DomainsUiState] with [DomainsUiContentState.Empty] content state.
     */
    private fun verifyEmptyItemsContentUiState(
        uiState: DomainsUiState,
        showClearButton: Boolean = false
    ) {
        assertThat(uiState.searchInputUiState.showProgress, Is(false))
        assertThat(uiState.searchInputUiState.showClearButton, Is(showClearButton))
        assertThat(uiState.contentState, instanceOf(DomainsUiContentState.Empty::class.java))
        assertThat(uiState.contentState.items.size, Is(0))
    }

    /**
     * Helper function that creates an [OnSuggestedDomains] event for the given query and number of results pair.
     */
    private fun createSuccessfulOnSuggestedDomains(queryResultSizePair: Pair<String, Int>): OnSuggestedDomains {
        val suggestions = (0..(queryResultSizePair.second - 1)).map {
            val response = DomainSuggestionResponse()
            response.domain_name = "${queryResultSizePair.first}-$it.wordpress.com"
            response
        }
        return OnSuggestedDomains(queryResultSizePair.first, suggestions)
    }

    /**
     * Helper function that creates an error [OnSuggestedDomains] event.
     */
    private fun createFailedOnSuggestedDomains(searchQuery: String): OnSuggestedDomains {
        val event = OnSuggestedDomains(searchQuery, emptyList())
        event.error = SuggestDomainError("GENERIC_ERROR", "test")
        return event
    }
}
