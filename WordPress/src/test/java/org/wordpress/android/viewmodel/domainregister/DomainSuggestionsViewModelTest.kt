package org.wordpress.android.viewmodel.domainregister

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.atLeast
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload

@RunWith(MockitoJUnitRunner::class)
class DomainSuggestionsViewModelTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()
    @Mock
    private lateinit var dispatcher: Dispatcher
    private var site: SiteModel = SiteModel()
    private val actionCaptor = argumentCaptor<Action<Any>>()

    private lateinit var viewModel: DomainSuggestionsViewModel

    init {
        site.name = "Wordpress"
    }

    @Before
    fun setUp() {
        viewModel = DomainSuggestionsViewModel(dispatcher)
        viewModel.site = site
    }

    @Test
    fun fetchSuggestionOnViewModelStart() {
        Assert.assertNull(viewModel.selectedSuggestion.value)
        Assert.assertNull(viewModel.selectedPosition.value)

        viewModel.start(site)
        assertFetchSuggestionsInvoked(site.name)
    }

    @Test
    fun fetchSuggestionOnSearchQueryChange() {
        val query = "sample site name"
        viewModel.updateSearchQuery(query)
        assertFetchSuggestionsInvoked(query)
    }

    @Test
    fun selectPositionOnSuggestionSelection() {
        val suggestions = getMockDomainSuggestions()

        viewModel.onDomainSuggestionsFetched(OnSuggestedDomains("", suggestions))
        // Select the second suggestion
        viewModel.onDomainSuggestionsSelected(suggestions[1], 1)
        validateSelection(suggestions[1], 1)

        // Select the first suggestion
        viewModel.onDomainSuggestionsSelected(suggestions[0], 0)
        validateSelection(suggestions[0], 0)

        // Deselect the first suggestion
        viewModel.onDomainSuggestionsSelected(null, -1)
        validateSelection(null, -1)
    }

    private fun validateSelection(selectedSuggestion: DomainSuggestionResponse?, selectedPosition: Int) {
        Assert.assertEquals(viewModel.selectedSuggestion.value, selectedSuggestion)
        Assert.assertEquals(viewModel.selectedPosition.value, selectedPosition)
    }

    @Test
    fun enableChooseDomainOnSuggestionSelection() {
        val suggestions = getMockDomainSuggestions()

        viewModel.onDomainSuggestionsFetched(OnSuggestedDomains("", suggestions))

        val enableChooseDomainObserver = mock<Observer<Boolean>>()
        viewModel.shouldEnableChooseDomain.observeForever(enableChooseDomainObserver)
        viewModel.onDomainSuggestionsSelected(suggestions[1], 1)
        viewModel.onDomainSuggestionsSelected(null, -1)
        viewModel.onDomainSuggestionsSelected(suggestions[0], 0)
        verify(enableChooseDomainObserver, atLeast(3)).onChanged(any())
    }

    private fun assertFetchSuggestionsInvoked(query: String) {
        verify(dispatcher).dispatch(actionCaptor.capture())

        val isLoadingObserver = mock<Observer<Boolean>>()
        viewModel.isLoadingInProgress.observeForever(isLoadingObserver)

        val action = actionCaptor.firstValue
        Assert.assertEquals(action.type, SiteAction.SUGGEST_DOMAINS)
        Assert.assertTrue(action.payload is SuggestDomainsPayload)
        (action.payload as SuggestDomainsPayload).apply {
            Assert.assertEquals(this.includeWordpressCom, false)
            Assert.assertEquals(this.onlyWordpressCom, false)
            Assert.assertEquals(this.includeDotBlogSubdomain, true)
            Assert.assertEquals(this.query, query)
        }
        verify(isLoadingObserver, atLeast(1)).onChanged(any())
    }

    private fun getMockDomainSuggestions(): List<DomainSuggestionResponse> {
        val suggestions = mutableListOf<DomainSuggestionResponse>()
        suggestions.add(getMockDomainSuggestion("samplesite1"))
        suggestions.add(getMockDomainSuggestion("samplesite2"))
        suggestions.add(getMockDomainSuggestion("samplesite3"))
        return suggestions
    }

    private fun getMockDomainSuggestion(siteName: String): DomainSuggestionResponse {
        val domainSuggestionResponse = DomainSuggestionResponse()
        domainSuggestionResponse.domain_name = "$siteName.com"
        domainSuggestionResponse.is_free = false
        domainSuggestionResponse.cost = "50 $"
        domainSuggestionResponse.product_id = "0"
        domainSuggestionResponse.product_slug = "sample_slug"
        domainSuggestionResponse.relevance = 1.0F
        return domainSuggestionResponse
    }
}
