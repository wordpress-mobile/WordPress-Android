package org.wordpress.android.ui.domains.management.newdomainsearch

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.domains.management.newdomainsearch.domainsfetcher.NewDomainsSearchRepository
import org.wordpress.android.ui.domains.management.newdomainsearch.domainsfetcher.ProposedDomain
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

private const val SEARCH_QUERY_DELAY_MS = 250L

@OptIn(FlowPreview::class)
@HiltViewModel
class NewDomainSearchViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val newDomainsSearchRepository: NewDomainsSearchRepository,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ScopedViewModel(mainDispatcher) {
    private val _actionEvents = MutableSharedFlow<ActionEvent>()
    val actionEvents: Flow<ActionEvent> = _actionEvents

    private val _uiStateFlow: MutableStateFlow<UiState> = MutableStateFlow(UiState.PopulatedDomains(emptyList()))
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private val debouncedQuery = MutableStateFlow("")

    init {
        analyticsTracker.track(AnalyticsTracker.Stat.DOMAIN_MANAGEMENT_SEARCH_FOR_A_DOMAIN_SCREEN_SHOWN)
        debouncedQuery
            .filter { it.isNotBlank() }
            .debounce(SEARCH_QUERY_DELAY_MS)
            .onEach(::fetchDomains)
            .launchIn(viewModelScope)
    }

    private suspend fun fetchDomains(query: String) {
        _uiStateFlow.emit(UiState.Loading)
        val result = newDomainsSearchRepository.searchForDomains(query)
        _uiStateFlow.emit(
            when (result) {
                is NewDomainsSearchRepository.DomainsResult.Success -> UiState.PopulatedDomains(result.proposedDomains)
                is NewDomainsSearchRepository.DomainsResult.Error -> UiState.Error
            }
        )
    }

    fun onSearchQueryChanged(query: String) {
        launch {
           debouncedQuery.emit(query.trim())
        }
    }

    fun onTransferDomainClicked() {
        analyticsTracker.track(AnalyticsTracker.Stat.DOMAIN_MANAGEMENT_TRANSFER_DOMAIN_TAPPED)
        launch {
            _actionEvents.emit(ActionEvent.TransferDomain(DOMAIN_TRANSFER_PAGE_URL))
        }
    }

    fun onDomainTapped(domain: ProposedDomain) {
        analyticsTracker.track(
            AnalyticsTracker.Stat.DOMAIN_MANAGEMENT_SEARCH_DOMAIN_TAPPED,
            mapOf("domain_name" to domain.domain)
        )
        launch {
            _actionEvents.emit(ActionEvent.PurchaseDomain(domain.domain))
        }
    }

    fun onBackPressed() {
        launch {
            _actionEvents.emit(ActionEvent.GoBack)
        }
    }

    sealed class ActionEvent {
        data class PurchaseDomain(val domain: String) : ActionEvent()
        data class TransferDomain(val url: String) : ActionEvent()
        object GoBack : ActionEvent()
    }

    sealed class UiState {
        object Error : UiState()
        object Loading : UiState()
        data class PopulatedDomains(val domains: List<ProposedDomain>) : UiState()
    }

    companion object {
        const val DOMAIN_TRANSFER_PAGE_URL = "https://wordpress.com/setup/domain-transfer/intro"
    }
}
