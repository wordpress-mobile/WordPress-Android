package org.wordpress.android.ui.sitecreation.domains

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState.Cost
import org.wordpress.android.ui.sitecreation.usecases.FetchDomainsUseCase
import kotlin.test.assertIs

private val queryToResultsSize = "multi_result_query" to 20

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteCreationDomainsViewModelNewUiTest : BaseUnitTest() {

    private val fetchDomainsUseCase = mock<FetchDomainsUseCase>()

    private lateinit var viewModel: SiteCreationDomainsViewModel

    @Before
    fun setUp() {
        viewModel = SiteCreationDomainsViewModel(
            networkUtils = mock() { on { isNetworkAvailable() }.thenReturn(true) },
            domainSanitizer = mock(),
            dispatcher = mock(),
            fetchDomainsUseCase = fetchDomainsUseCase,
            purchasingFeatureConfig = mock() { whenever(it.isEnabledOrManuallyOverridden()).thenReturn(true) },
            tracker = mock(),
            bgDispatcher = testDispatcher(),
            mainDispatcher = testDispatcher(),
        )
    }

    @Test
    fun `all domain results from api should be visible`() = testWithSuccessResult { (query, results) ->
        viewModel.start()

        viewModel.onQueryChanged(query)
        advanceUntilIdle()

        val uiDomains = assertIs<List<DomainUiState>>(viewModel.uiState.value!!.contentState.items)
        assertThat(uiDomains).hasSameSizeAs(results)
    }

    @Test
    fun `free domain results from api should be with 'Free' cost`() = testWithSuccessResult { (query, results) ->
        val apiFreeDomains = results.filter { it.is_free }
        viewModel.start()

        viewModel.onQueryChanged(query)
        advanceUntilIdle()

        val uiFreeDomains = viewModel.uiState.value?.contentState?.items
            ?.filter { (it as DomainUiState).cost is Cost.Free }
        assertThat(uiFreeDomains).hasSameSizeAs(apiFreeDomains)
    }

    @Test
    fun `paid domain results from api should be with 'Paid' cost`() = testWithSuccessResult { (query, results) ->
        val apiPaidDomains = results.filter { !it.is_free }
        viewModel.start()

        viewModel.onQueryChanged(query)
        advanceUntilIdle()

        val uiPaidDomains = viewModel.uiState.value?.contentState?.items
            ?.filter { (it as DomainUiState).cost is Cost.Paid }
        assertThat(uiPaidDomains).hasSameSizeAs(apiPaidDomains)
    }

    private fun <T> testWithSuccessResult(
        queryToSize: Pair<String, Int> = queryToResultsSize,
        block: suspend CoroutineScope.(OnSuggestedDomains) -> T
    ) {
        test {
            val (query, size) = queryToSize

            val suggestions = (0 until size).map {
                DomainSuggestionResponse().apply {
                    domain_name = "$query-$it.com"
                    is_free = it % 2 == 0
                    cost = if (is_free) "Free" else "$$it.00"
                }
            }

            val event = OnSuggestedDomains(query, suggestions)
            whenever(fetchDomainsUseCase.fetchDomains(any(), any(), any(), any())).thenReturn(event)
            block(event)
        }
    }
}
