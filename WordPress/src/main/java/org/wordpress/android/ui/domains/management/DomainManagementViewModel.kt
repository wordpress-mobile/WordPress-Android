package org.wordpress.android.ui.domains.management

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.network.rest.wpcom.site.AllDomainsDomain
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainStatus
import org.wordpress.android.fluxc.network.rest.wpcom.site.StatusType
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.domains.management.util.DomainLocalSearchEngine
import org.wordpress.android.ui.domains.usecases.AllDomains
import org.wordpress.android.ui.domains.usecases.FetchAllDomainsUseCase
import org.wordpress.android.ui.themes.neutral
import org.wordpress.android.ui.themes.success
import org.wordpress.android.ui.themes.warning
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class DomainManagementViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val fetchAllDomainsUseCase: FetchAllDomainsUseCase,
    private val domainLocalSearchEngine: DomainLocalSearchEngine,
) : ScopedViewModel(mainDispatcher) {
    private val _actionEvents = MutableSharedFlow<ActionEvent>()
    val actionEvents: Flow<ActionEvent> = _actionEvents

    private val _uiStateFlow: MutableStateFlow<UiState> = MutableStateFlow(UiState.PopulatedList.Initial)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        analyticsTracker.track(Stat.DOMAIN_MANAGEMENT_DOMAINS_LIST_SHOWN)
        launch {
            fetchAllDomains()
        }
        launch {
            searchQuery
                .onEach(::filterDomains)
                .launchIn(viewModelScope)
        }
    }

    private suspend fun fetchAllDomains() {
        fetchAllDomainsUseCase.execute().let {
            _uiStateFlow.value = when (it) {
                AllDomains.Empty -> UiState.Empty
                AllDomains.Error -> UiState.Error
                is AllDomains.Success -> UiState.PopulatedList.Loaded.Complete(it.domains)
            }
        }
    }

    private fun filterDomains(query: String) {
        val domains = when (val state = _uiStateFlow.value) {
            is UiState.PopulatedList.Loaded -> state.allDomains
            else -> return
        }
        _uiStateFlow.value = if (query.isBlank()) {
            UiState.PopulatedList.Loaded.Complete(allDomains = domains)
        } else {
            UiState.PopulatedList.Loaded.Filtered(
                filtered = domainLocalSearchEngine.filter(domains, query),
                allDomains = domains
            )
        }
    }

    fun onDomainTapped(domain: String, detailUrl: String) {
        launch {
            _actionEvents.emit(ActionEvent.DomainTapped(domain, detailUrl))
        }
    }

    fun onAddDomainClicked() {
        analyticsTracker.track(Stat.DOMAIN_MANAGEMENT_ADD_DOMAIN_TAPPED)
        launch {
            _actionEvents.emit(ActionEvent.AddDomainTapped)
        }
    }

    fun onBackTapped() {
        launch {
            _actionEvents.emit(ActionEvent.NavigateBackTapped)
        }
    }

    fun onRefresh() {
        launch {
            _uiStateFlow.value = UiState.PopulatedList.Initial
            fetchAllDomains()
        }
    }

    fun onSearchQueryChanged(query: String) {
        launch {
            searchQuery.emit(query.trim())
        }
    }

    sealed class ActionEvent {
        data class DomainTapped(val domain: String, val detailUrl: String): ActionEvent()
        object AddDomainTapped: ActionEvent()
        object NavigateBackTapped: ActionEvent()
    }

    sealed class UiState {
        sealed class PopulatedList: UiState() {
            object Initial: PopulatedList()
            sealed class Loaded : PopulatedList() {
                abstract val allDomains: List<AllDomainsDomain>
                data class Complete(
                    override val allDomains: List<AllDomainsDomain>
                ): Loaded()
                data class Filtered(
                    override val allDomains: List<AllDomainsDomain>,
                    val filtered: List<AllDomainsDomain>
                ) : Loaded()
            }
        }
        object Empty: UiState()
        object Error: UiState()
    }
}

sealed class DomainCardUiState {
    object Initial: DomainCardUiState()
    data class Loaded(
        val domain: String?,
        val title: String?,
        val detailUrl: String?,
        val statusUiState: StatusRowUiState,
    ): DomainCardUiState()

    companion object {
        @Composable
        fun fromDomain(domain: AllDomainsDomain?) = (domain ?: AllDomainsDomain()).let {
            val domainStatus = it.domainStatus ?: DomainStatus()
            Loaded(
                domain = it.domain,
                title = it.blogName,
                detailUrl = it.getDomainDetailsUrl(),
                statusUiState = StatusRowUiState.Loaded(
                    indicatorColor = domainStatus.indicatorColor,
                    statusText = domainStatus.statusText,
                    textColor = domainStatus.textColor,
                    isBold = domainStatus.isBold,
                    expiry = it.expiry?.toLocalDate(),
                )
            )
        }
    }
}

sealed class StatusRowUiState {
    object Initial: StatusRowUiState()
    data class Loaded(
        val indicatorColor: Color,
        val statusText: String,
        val textColor: Color,
        val isBold: Boolean = false,
        val expiry: LocalDate?,
    ): StatusRowUiState()
}

private fun Date.toLocalDate(zoneId: ZoneId = ZoneId.systemDefault()) =
    toInstant().atZone(zoneId).toLocalDate()
val DomainStatus.indicatorColor
    @Composable
    get() = when (statusType) {
        StatusType.SUCCESS -> MaterialTheme.colorScheme.success
        StatusType.NEUTRAL -> MaterialTheme.colorScheme.neutral
        StatusType.ALERT -> MaterialTheme.colorScheme.error
        StatusType.WARNING -> MaterialTheme.colorScheme.warning
        StatusType.ERROR -> MaterialTheme.colorScheme.error
        StatusType.UNKNOWN -> MaterialTheme.colorScheme.error
        null ->  MaterialTheme.colorScheme.error
    }

val DomainStatus.statusText
    @Composable
    get() = status ?: stringResource(id = R.string.error)

val DomainStatus.textColor
    @Composable
    get() = when (statusType) {
        StatusType.ERROR,
        StatusType.UNKNOWN,
        null -> MaterialTheme.colorScheme.error
        else -> LocalTextStyle.current.color
    }

val DomainStatus.isBold
    @Composable
    get() = when (statusType) {
        StatusType.ERROR,
        StatusType.UNKNOWN,
        null -> true
        else -> false
    }
