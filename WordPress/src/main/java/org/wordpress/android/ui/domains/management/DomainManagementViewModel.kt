package org.wordpress.android.ui.domains.management

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.network.rest.wpcom.site.AllDomainsDomain
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.domains.usecases.AllDomains
import org.wordpress.android.ui.domains.usecases.FetchAllDomainsUseCase
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class DomainManagementViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val fetchAllDomainsUseCase: FetchAllDomainsUseCase,
) : ScopedViewModel(mainDispatcher) {
    private val _actionEvents = MutableSharedFlow<ActionEvent>()
    val actionEvents: Flow<ActionEvent> = _actionEvents

    private val _uiStateFlow: MutableStateFlow<UiState> = MutableStateFlow(UiState.PopulatedList.Initial)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    init {
        analyticsTracker.track(Stat.DOMAIN_MANAGEMENT_MY_DOMAINS_SCREEN_SHOWN)
        launch {
            fetchAllDomainsUseCase.execute().let {
                _uiStateFlow.value = when (it) {
                    AllDomains.Empty -> UiState.Empty
                    AllDomains.Error -> UiState.Error
                    is AllDomains.Success -> UiState.PopulatedList.Loaded(it.domains)
                }
            }
        }
    }

    sealed class ActionEvent {
        object DomainTapped: ActionEvent()
    }

    sealed class UiState {
        sealed class PopulatedList: UiState() {
            object Initial: PopulatedList()
            data class Loaded(val domains: List<AllDomainsDomain>): PopulatedList()
        }
        object Empty: UiState()
        object Error: UiState()
    }
}
