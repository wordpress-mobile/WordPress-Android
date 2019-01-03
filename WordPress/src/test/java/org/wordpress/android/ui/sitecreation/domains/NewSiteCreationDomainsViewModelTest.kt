package org.wordpress.android.ui.sitecreation.domains

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import com.nhaarman.mockito_kotlin.firstValue
import com.nhaarman.mockito_kotlin.secondValue
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.CoroutineScope
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
import org.wordpress.android.test
import org.wordpress.android.ui.sitecreation.domains.NewSiteCreationDomainsViewModel.DomainsUiState
import org.wordpress.android.ui.sitecreation.domains.NewSiteCreationDomainsViewModel.DomainsUiState.DomainsUiContentState
import org.wordpress.android.ui.sitecreation.usecases.FetchDomainsUseCase
import org.wordpress.android.util.NetworkUtilsWrapper
import org.hamcrest.CoreMatchers.`is` as Is

private const val MULTI_RESULT_DOMAIN_FETCH_RESULT_SIZE = 20
private val MULTI_RESULT_DOMAIN_FETCH_QUERY = Pair("multi_result_query", MULTI_RESULT_DOMAIN_FETCH_RESULT_SIZE)
private val EMPTY_RESULT_DOMAIN_FETCH_QUERY = Pair("empty_result_query", 0)

@RunWith(MockitoJUnitRunner::class)
class NewSiteCreationDomainsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var fetchDomainsUseCase: FetchDomainsUseCase
    @Mock private lateinit var uiStateObserver: Observer<DomainsUiState>
    @Mock private lateinit var createSiteBtnObserver: Observer<String>
    @Mock private lateinit var clearBtnObserver: Observer<Unit>
    @Mock private lateinit var onHelpClickedObserver: Observer<Unit>
    @Mock private lateinit var onInputFocusRequestedObserver: Observer<Unit>
    @Mock private lateinit var networkUtils: NetworkUtilsWrapper

    private lateinit var viewModel: NewSiteCreationDomainsViewModel

    @Before
    fun setUp() {
        viewModel = NewSiteCreationDomainsViewModel(
                networkUtils = networkUtils,
                dispatcher = dispatcher,
                fetchDomainsUseCase = fetchDomainsUseCase,
                IO = TEST_DISPATCHER,
                MAIN = TEST_DISPATCHER
        )
        viewModel.uiState.observeForever(uiStateObserver)
        viewModel.createSiteBtnClicked.observeForever(createSiteBtnObserver)
        viewModel.clearBtnClicked.observeForever(clearBtnObserver)
        viewModel.onHelpClicked.observeForever(onHelpClickedObserver)
        viewModel.onInputFocusRequested.observeForever(onInputFocusRequestedObserver)
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

    @Test
    fun verifyEmptyTitleQueryUiState() = testWithSuccessResponse {
        viewModel.start(null)
        verifyInitialContentUiState(requireNotNull(viewModel.uiState.value), showProgress = false)
    }

    @Test
    fun verifyMultiResultTitleQueryInitialUiState() = testWithSuccessResponse {
        viewModel.start(MULTI_RESULT_DOMAIN_FETCH_QUERY.first)
        val captor = ArgumentCaptor.forClass(DomainsUiState::class.java)
        verify(uiStateObserver, times(2)).onChanged(captor.capture())
        verifyInitialContentUiState(requireNotNull(captor.firstValue), showProgress = true)
    }

    @Test
    fun verifyMultiResultTitleQueryUiStateAfterResponse() = testWithSuccessResponse {
        viewModel.start(MULTI_RESULT_DOMAIN_FETCH_QUERY.first)
        val captor = ArgumentCaptor.forClass(DomainsUiState::class.java)
        verify(uiStateObserver, times(2)).onChanged(captor.capture())
        verifyVisibleItemsContentUiState(captor.secondValue)
    }

    @Test
    fun verifyUpdateEmptyQueryInitialUiState() =
            testWithSuccessResponse(queryResultSizePair = EMPTY_RESULT_DOMAIN_FETCH_QUERY) {
                viewModel.updateQuery(EMPTY_RESULT_DOMAIN_FETCH_QUERY.first)
                val captor = ArgumentCaptor.forClass(DomainsUiState::class.java)
                verify(uiStateObserver, times(2)).onChanged(captor.capture())
                verifyInitialContentUiState(captor.firstValue, showProgress = true, showClearButton = true)
            }

    @Test
    fun verifyUpdateEmptyQueryUiStateAfterResponse() =
            testWithSuccessResponse(queryResultSizePair = EMPTY_RESULT_DOMAIN_FETCH_QUERY) {
                viewModel.updateQuery(EMPTY_RESULT_DOMAIN_FETCH_QUERY.first)
                val captor = ArgumentCaptor.forClass(DomainsUiState::class.java)
                verify(uiStateObserver, times(2)).onChanged(captor.capture())
                verifyEmptyItemsContentUiState(
                        captor.secondValue,
                        showClearButton = true
                )
            }

    @Test
    fun verifyUpdateMultiResultQueryInitialUiState() = testWithSuccessResponse {
        viewModel.updateQuery(MULTI_RESULT_DOMAIN_FETCH_QUERY.first)
        val captor = ArgumentCaptor.forClass(DomainsUiState::class.java)
        verify(uiStateObserver, times(2)).onChanged(captor.capture())
        verifyInitialContentUiState(captor.firstValue, showProgress = true, showClearButton = true)
    }

    @Test
    fun verifyUpdateMultiResultQueryUiStateAfterResponse() = testWithSuccessResponse {
        viewModel.updateQuery(MULTI_RESULT_DOMAIN_FETCH_QUERY.first)
        val captor = ArgumentCaptor.forClass(DomainsUiState::class.java)
        verify(uiStateObserver, times(2)).onChanged(captor.capture())
        verifyVisibleItemsContentUiState(captor.secondValue, showClearButton = true)
    }

    @Test
    fun verifyOnHelpClickedPropagated() = testWithSuccessResponse {
        viewModel.onHelpClicked()
        val captor = ArgumentCaptor.forClass(Unit::class.java)
        verify(onHelpClickedObserver, times(1)).onChanged(captor.capture())
    }

    @Test
    fun verifyOnClearBtnClickedPropagated() = testWithSuccessResponse {
        viewModel.onClearTextBtnClicked()
        val captor = ArgumentCaptor.forClass(Unit::class.java)
        verify(clearBtnObserver, times(1)).onChanged(captor.capture())
    }

    @Test
    fun verifyCreateSiteBtnClickedPropagated() = testWithSuccessResponse {
        val domainName = "test.domain"
        viewModel.setSelectedDomainName(domainName)
        viewModel.createSiteBtnClicked()
        val captor = ArgumentCaptor.forClass(String::class.java)
        verify(createSiteBtnObserver, times(1)).onChanged(captor.capture())
        assertThat(captor.firstValue, Is(domainName))
    }

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

    private fun verifyEmptyItemsContentUiState(
        uiState: DomainsUiState,
        showClearButton: Boolean = false
    ) {
        assertThat(uiState.searchInputUiState.showProgress, Is(false))
        assertThat(uiState.searchInputUiState.showClearButton, Is(showClearButton))
        assertThat(uiState.contentState, instanceOf(DomainsUiContentState.Empty::class.java))
        assertThat(uiState.contentState.items.size, Is(0))
    }

    private fun createSuccessfulOnSuggestedDomains(queryResultSizePair: Pair<String, Int>): OnSuggestedDomains {
        val suggestions = (0..(queryResultSizePair.second - 1)).map {
            val response = DomainSuggestionResponse()
            response.domain_name = "${queryResultSizePair.first}-$it.wordpress.com"
            response
        }
        return OnSuggestedDomains(queryResultSizePair.first, suggestions)
    }
}
