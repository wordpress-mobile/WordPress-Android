package org.wordpress.android.ui.domains.management.newdomainsearch

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
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
    private lateinit var repository: NewDomainsSearchRepository

    private lateinit var viewModel: NewDomainSearchViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = NewDomainSearchViewModel(
            mainDispatcher = testDispatcher(),
            analyticsTracker = analyticsTracker,
            newDomainsSearchRepository = repository
        )
    }

    @Test
    fun `WHEN ViewModel initialized THEN track DOMAIN_MANAGEMENT_SEARCH_FOR_A_DOMAIN_SCREEN_SHOWN event`() {
        verify(analyticsTracker).track(AnalyticsTracker.Stat.DOMAIN_MANAGEMENT_SEARCH_FOR_A_DOMAIN_SCREEN_SHOWN)
    }

    @Test
    fun `WHEN a domain is tapped THEN send PurchaseDomain action event`() = testWithActionEvents { events ->
        val domain = ProposedDomain(1, "test.com", "1", null)
        viewModel.onDomainTapped(domain)
        advanceUntilIdle()

        val expectedEvent = ActionEvent.PurchaseDomain(domain.domain)
        assertThat(events.last()).isEqualTo(expectedEvent)
    }

    @Test
    fun `WHEN a domain is tapped THEN track DOMAIN_MANAGEMENT_SEARCH_DOMAIN_TAPPED event`() = test {
        val domain = ProposedDomain(1, "test.com", "1", null)
        viewModel.onDomainTapped(domain)
        advanceUntilIdle()

        verify(analyticsTracker)
            .track(
                eq(AnalyticsTracker.Stat.DOMAIN_MANAGEMENT_SEARCH_DOMAIN_TAPPED),
                argWhere<Map<String, Any?>> {
                    assertThat(it).hasSize(1)
                    assertThat(it).containsEntry("domain_name", "test.com")
                    true
                }
            )
    }

    @Test
    fun `WHEN transfer domain button pressed THEN send TransferDomain action event`() = testWithActionEvents { events ->
        viewModel.onTransferDomainClicked()
        advanceUntilIdle()

        val expectedTransferUrl = "https://wordpress.com/setup/domain-transfer/intro"
        assertThat(events.last()).isEqualTo(ActionEvent.TransferDomain(expectedTransferUrl))
    }

    @Test
    fun `WHEN transfer domain button pressed THEN track DOMAIN_MANAGEMENT_TRANSFER_DOMAIN_TAPPED event`() = test {
            viewModel.onTransferDomainClicked()
            advanceUntilIdle()

            verify(analyticsTracker).track(AnalyticsTracker.Stat.DOMAIN_MANAGEMENT_TRANSFER_DOMAIN_TAPPED)
        }

    @Test
    fun `WHEN back button pressed THEN send GoBack action event`() = testWithActionEvents { events ->
        viewModel.onBackPressed()
        advanceUntilIdle()

        assertThat(events.last()).isEqualTo(ActionEvent.GoBack)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `GIVEN few queries requested within 250 ms query delay WHEN onSearchQueryChanged THEN search for domains with the last query only`() =
        test {
            whenever(repository.searchForDomains("third")).thenReturn(DomainsResult.Success(emptyList()))

            viewModel.onSearchQueryChanged("first")
            delay(200)
            viewModel.onSearchQueryChanged("second")
            delay(249)
            viewModel.onSearchQueryChanged("third")
            advanceUntilIdle()

            verify(repository, never()).searchForDomains("first")
            verify(repository, never()).searchForDomains("second")
            verify(repository).searchForDomains("third")
        }

    @Suppress("MaxLineLength")
    @Test
    fun `GIVEN few queries requested outside 250 ms delay WHEN onSearchQueryChanged THEN search for domains with all the queries`() =
        test {
            whenever(repository.searchForDomains("first")).thenReturn(DomainsResult.Success(emptyList()))
            whenever(repository.searchForDomains("second")).thenReturn(DomainsResult.Success(emptyList()))
            whenever(repository.searchForDomains("third")).thenReturn(DomainsResult.Success(emptyList()))

            viewModel.onSearchQueryChanged("first")
            delay(250)
            viewModel.onSearchQueryChanged("second")
            delay(300)
            viewModel.onSearchQueryChanged("third")
            advanceUntilIdle()

            verify(repository).searchForDomains("first")
            verify(repository).searchForDomains("second")
            verify(repository).searchForDomains("third")
        }

    @Test
    fun `GIVEN successful search for domain result WHEN onSearchQueryChanged THEN emit populated domains UI state`() =
        testWithUiStates { states ->
            val domains = listOf(
                ProposedDomain(
                    productId = 0,
                    domain = "domain.com",
                    price = "USD 100",
                    salePrice = null
                )
            )
            val result = DomainsResult.Success(domains)
            whenever(repository.searchForDomains("query")).thenReturn(result)

            viewModel.onSearchQueryChanged("query")
            advanceUntilIdle()

            assertThat(states.last()).isEqualTo(UiState.PopulatedDomains(domains = domains))
        }

    @Test
    fun `GIVEN error search for domain result WHEN onSearchQueryChanged THEN emit error UI state`() =
        testWithUiStates { states ->
            whenever(repository.searchForDomains("query")).thenReturn(DomainsResult.Error)

            viewModel.onSearchQueryChanged("query")
            advanceUntilIdle()

            assertThat(states.last()).isEqualTo(UiState.Error)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `GIVEN blank and empty queries WHEN onSearchQueryChanged THEN do not fetch domains`() = test {
        viewModel.onSearchQueryChanged("")
        delay(250)
        viewModel.onSearchQueryChanged(" ")
        advanceUntilIdle()

        verifyNoInteractions(repository)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `GIVEN repeated queries with redundant blank symbols WHEN onSearchQueryChanged THEN fetch domains only once`() = test {
        whenever(repository.searchForDomains("query")).thenReturn(DomainsResult.Success(emptyList()))

        viewModel.onSearchQueryChanged("query")
        delay(250)
        viewModel.onSearchQueryChanged(" query")
        delay(250)
        viewModel.onSearchQueryChanged("query ")
        advanceUntilIdle()

        verify(repository, times(1)).searchForDomains("query")
        verify(repository, never()).searchForDomains(" query")
        verify(repository, never()).searchForDomains("query ")
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
