package org.wordpress.android.ui.domains.management

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_MANAGEMENT_MY_DOMAINS_SCREEN_DOMAIN_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_MANAGEMENT_MY_DOMAINS_SCREEN_SHOWN
import org.wordpress.android.fluxc.network.rest.wpcom.site.AllDomainsDomain
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.AllDomainsError
import org.wordpress.android.fluxc.store.SiteStore.AllDomainsErrorType
import org.wordpress.android.fluxc.store.SiteStore.FetchedAllDomainsPayload
import org.wordpress.android.ui.domains.management.DomainManagementViewModel.ActionEvent
import org.wordpress.android.ui.domains.management.DomainManagementViewModel.UiState
import org.wordpress.android.ui.domains.management.util.DomainLocalSearchEngine
import org.wordpress.android.ui.domains.usecases.AllDomains
import org.wordpress.android.ui.domains.usecases.FetchAllDomainsUseCase
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@ExperimentalCoroutinesApi
class DomainManagementViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper

    @Mock
    lateinit var useCase: FetchAllDomainsUseCase

    @Mock
    lateinit var siteStore: SiteStore

    @Mock
    lateinit var domainLocalSearchEngine: DomainLocalSearchEngine

    private lateinit var viewModel: DomainManagementViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `WHEN ViewModel initialized THEN track DOMAIN_MANAGEMENT_MY_DOMAINS_SCREEN_SHOWN event`() = test {
        initializeViewModel()
        verify(analyticsTracker).track(DOMAIN_MANAGEMENT_MY_DOMAINS_SCREEN_SHOWN)
    }

    @Test
    fun `WHEN a domain is tapped THEN track DOMAIN_MANAGEMENT_MY_DOMAINS_SCREEN_DOMAIN_TAPPED event`() = test {
        initializeViewModel()
        viewModel.onDomainTapped(testDomain)
        verify(analyticsTracker).track(DOMAIN_MANAGEMENT_MY_DOMAINS_SCREEN_DOMAIN_TAPPED)
    }

    @Test
    fun `WHEN a domain is tapped THEN send DomainTapped action event`() = testWithActionEvents { events ->
        viewModel.onDomainTapped(testDomain)
        advanceUntilIdle()
        assertThat(events.last()).isEqualTo(ActionEvent.DomainTapped(testDomain))
    }

    @Test
    fun `WHEN a navigation back button is tapped THEN send NavigateBackTapped action event`() =
        testWithActionEvents { events ->
            viewModel.onBackTapped()
            advanceUntilIdle()
            assertThat(events.last()).isEqualTo(ActionEvent.NavigateBackTapped)
        }

    @Test
    fun `GIVEN empty domains result WHEN initialized THEN send empty UI state`() = testWithState { state ->
        assertThat(state.last()).isEqualTo(UiState.Empty)
    }

    @Test
    fun `GIVEN error domains result WHEN initialized THEN send error UI state`() =
        testWithState(initialAllDomainsFetchResult = AllDomains.Error) { state ->
            assertThat(state.last()).isEqualTo(UiState.Error)
        }

    @Test
    fun `GIVEN successful domains result WHEN initialized THEN send populated list loaded complete UI state`() {
        val domains = listOf(AllDomainsDomain(), AllDomainsDomain())
        testWithState(initialAllDomainsFetchResult = AllDomains.Success(domains)) { state ->
            assertThat(state.last()).isEqualTo(UiState.PopulatedList.Loaded.Complete(domains))
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `GIVEN successful domains result and query is not blank WHEN search query changed THEN return populated loaded filtered UI state`() {
        val domains = listOf(AllDomainsDomain(), AllDomainsDomain())
        val filteredDomains = listOf(AllDomainsDomain())
        whenever(domainLocalSearchEngine.filter(domains, query = "query")).thenReturn(filteredDomains)

        testWithState(initialAllDomainsFetchResult = AllDomains.Success(domains)) { state ->
            viewModel.onSearchQueryChanged("query")
            advanceUntilIdle()
            assertThat(state.last()).isEqualTo(
                UiState.PopulatedList.Loaded.Filtered(allDomains = domains, filtered = filteredDomains)
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `GIVEN successful domains result and blank query WHEN search query changed THEN return populated loaded complete UI state`() {
        val domains = listOf(AllDomainsDomain(), AllDomainsDomain())

        testWithState(initialAllDomainsFetchResult = AllDomains.Success(domains)) { state ->
            viewModel.onSearchQueryChanged(" ")
            advanceUntilIdle()
            assertThat(state.last()).isEqualTo(UiState.PopulatedList.Loaded.Complete(allDomains = domains))
            verifyNoInteractions(domainLocalSearchEngine)
        }
    }

    @Test
    fun `onRefresh fetches all the domains again`() = test {
        // Given
        val useCase = FetchAllDomainsUseCase(siteStore)
        whenever(siteStore.fetchAllDomains()).thenReturn(
            FetchedAllDomainsPayload(AllDomainsError(AllDomainsErrorType.GENERIC_ERROR)),
            FetchedAllDomainsPayload(emptyList()),
        )
        viewModel = DomainManagementViewModel(testDispatcher(), analyticsTracker, useCase, domainLocalSearchEngine)

        // When
        viewModel.onRefresh()

        // Then
        verify(siteStore, times(2)).fetchAllDomains()
    }

    private fun testWithActionEvents(
        initialAllDomainsFetchResult: AllDomains = AllDomains.Empty,
        block: suspend TestScope.(events: List<ActionEvent>) -> Unit
    ) = test {
        initializeViewModel(initialAllDomainsFetchResult)
        val actionEvents = mutableListOf<ActionEvent>()
        val job = launch { viewModel.actionEvents.toList(actionEvents) }

        block(actionEvents)

        job.cancel()
    }

    private fun testWithState(
        initialAllDomainsFetchResult: AllDomains = AllDomains.Empty,
        block: suspend TestScope.(states: List<UiState>) -> Unit
    ) = test {
        initializeViewModel(initialAllDomainsFetchResult)
        val states = mutableListOf<UiState>()
        val job = launch { viewModel.uiStateFlow.toList(states) }

        block(states)

        job.cancel()
    }

    private suspend fun initializeViewModel(initialAllDomainsFetchResult: AllDomains = AllDomains.Empty) {
        whenever(useCase.execute()).thenReturn(initialAllDomainsFetchResult)
        viewModel = DomainManagementViewModel(testDispatcher(), analyticsTracker, useCase, domainLocalSearchEngine)
    }

    companion object {
        private const val testDomain = "domain"
    }
}
