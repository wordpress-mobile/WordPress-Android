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
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class DomainManagementViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ScopedViewModel(mainDispatcher) {
    private val _actionEvents = MutableSharedFlow<ActionEvent>()
    val actionEvents: Flow<ActionEvent> = _actionEvents

    private val _uiStateFlow = MutableStateFlow(UiState.Initial)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    init {
        analyticsTracker.track(Stat.DOMAIN_MANAGEMENT_MY_DOMAINS_SCREEN_SHOWN)
    }


    sealed class ActionEvent {
        object DomainTapped: ActionEvent()
    }

    sealed class UiState(
        val domains: List<AllDomainsDomain>,
    ) {
        object Initial: UiState(
            domains = List(2) { AllDomainsDomain() }
        )
    }
}
