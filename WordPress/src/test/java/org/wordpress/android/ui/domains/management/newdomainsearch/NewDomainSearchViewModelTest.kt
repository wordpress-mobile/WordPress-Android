package org.wordpress.android.ui.domains.management.newdomainsearch

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.domains.management.newdomainsearch.NewDomainSearchViewModel.ActionEvent
import org.wordpress.android.ui.domains.management.newdomainsearch.NewDomainSearchViewModel.UiState
import org.wordpress.android.ui.domains.management.newdomainsearch.domainsfetcher.NewDomainsSearchRepository
import org.wordpress.android.ui.domains.management.newdomainsearch.domainsfetcher.NewDomainsSearchRepository.DomainsResult
import org.wordpress.android.ui.domains.management.newdomainsearch.domainsfetcher.ProposedDomain
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@ExperimentalCoroutinesApi
class NewDomainSearchViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper

    @Mock
    private lateinit var newDomainsSearchRepository: NewDomainsSearchRepository

    private lateinit var viewModel: NewDomainSearchViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = NewDomainSearchViewModel(
            mainDispatcher = testDispatcher(),
            analyticsTracker = analyticsTracker,
            newDomainsSearchRepository = newDomainsSearchRepository
        )
    }

    @Test
    fun `WHEN ViewModel initialized THEN track DOMAIN_MANAGEMENT_SEARCH_FOR_A_DOMAIN_SCREEN_SHOWN event`() {
        verify(analyticsTracker).track(AnalyticsTracker.Stat.DOMAIN_MANAGEMENT_SEARCH_FOR_A_DOMAIN_SCREEN_SHOWN)
    }

    @Test
    fun `WHEN back button pressed THEN send GoBack action event`() = testWithActionEvents { events ->
        viewModel.onBackPressed()
        advanceUntilIdle()

        assertThat(events.last()).isEqualTo(ActionEvent.GoBack)
    }

    @Test
    fun `GIVEN few queries requested within 250 ms query delay WHEN onSearchQueryChanged THEN search for domains with the last query only`() =
        test {
            whenever(newDomainsSearchRepository.searchForDomains("third")).thenReturn(DomainsResult.Success(emptyList()))

            viewModel.onSearchQueryChanged("first")
            delay(200)
            viewModel.onSearchQueryChanged("second")
            delay(249)
            viewModel.onSearchQueryChanged("third")
            advanceUntilIdle()

            verify(newDomainsSearchRepository, never()).searchForDomains("first")
            verify(newDomainsSearchRepository, never()).searchForDomains("second")
            verify(newDomainsSearchRepository).searchForDomains("third")
        }

    @Test
    fun `GIVEN few queries requested outside 250 ms delay WHEN onSearchQueryChanged THEN search for domains with all the queries`() =
        test {
            whenever(newDomainsSearchRepository.searchForDomains("first")).thenReturn(DomainsResult.Success(emptyList()))
            whenever(newDomainsSearchRepository.searchForDomains("second")).thenReturn(DomainsResult.Success(emptyList()))
            whenever(newDomainsSearchRepository.searchForDomains("third")).thenReturn(DomainsResult.Success(emptyList()))

            viewModel.onSearchQueryChanged("first")
            delay(250)
            viewModel.onSearchQueryChanged("second")
            delay(300)
            viewModel.onSearchQueryChanged("third")
            advanceUntilIdle()

            verify(newDomainsSearchRepository).searchForDomains("first")
            verify(newDomainsSearchRepository).searchForDomains("second")
            verify(newDomainsSearchRepository).searchForDomains("third")
        }

    @Test
    fun `GIVEN successful search for domain result WHEN onSearchQueryChanged THEN emit populated domains UI state`() =
        testWithUiStates { states ->
            val domains = listOf(
                ProposedDomain(
                    productId = 0,
                    domainPrefix = "domain",
                    domainSuffix = "com",
                    price = "USD 100",
                    salePrice = null
                )
            )
            val result = DomainsResult.Success(domains)
            whenever(newDomainsSearchRepository.searchForDomains("query")).thenReturn(result)

            viewModel.onSearchQueryChanged("query")
            advanceUntilIdle()

            assertThat(states.last()).isEqualTo(UiState.PopulatedDomains(domains = domains))
        }

    @Test
    fun `GIVEN error search for domain result WHEN onSearchQueryChanged THEN emit error UI state`() =
        testWithUiStates { states ->
            whenever(newDomainsSearchRepository.searchForDomains("query")).thenReturn(DomainsResult.Error)

            viewModel.onSearchQueryChanged("query")
            advanceUntilIdle()

            assertThat(states.last()).isEqualTo(UiState.Error)
        }

    private fun testWithActionEvents(block: suspend TestScope.(events: List<ActionEvent>) -> Unit) = test {
        val actionEvents = mutableListOf<ActionEvent>()
        val job = launch { viewModel.actionEvents.toList(actionEvents) }

        block(actionEvents)

        job.cancel()
    }

    private fun testWithUiStates(block: suspend TestScope.(events: List<UiState>) -> Unit) = test {
        val uiStates = mutableListOf<UiState>()
        val job = launch { viewModel.uiStateFlow.toList(uiStates) }

        block(uiStates)

        job.cancel()
    }
}
