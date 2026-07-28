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
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.domains.management.util.DomainLocalSearchEngine
import org.wordpress.android.ui.domains.usecases.AllDomains
import org.wordpress.android.ui.domains.usecases.FetchAllDomainsUseCase
import org.wordpress.android.ui.compose.theme.neutral
import org.wordpress.android.ui.compose.theme.success
import org.wordpress.android.ui.compose.theme.warning
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import uniffi.wp_api.AllDomainItem
import uniffi.wp_api.DomainListItemStatus
import uniffi.wp_api.DomainListItemStatusType
import java.time.LocalDate
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

    private val _uiStateFlow: MutableStateFlow<UiState> =
        MutableStateFlow(UiState.PopulatedList.Initial)
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
                is AllDomains.Success ->
                    UiState.PopulatedList.Loaded.Complete(it.domains)
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
        data class DomainTapped(
            val domain: String,
            val detailUrl: String
        ) : ActionEvent()
        object AddDomainTapped : ActionEvent()
        object NavigateBackTapped : ActionEvent()
    }

    sealed class UiState {
        sealed class PopulatedList : UiState() {
            object Initial : PopulatedList()
            sealed class Loaded : PopulatedList() {
                abstract val allDomains: List<AllDomainItem>
                data class Complete(
                    override val allDomains: List<AllDomainItem>
                ) : Loaded()
                data class Filtered(
                    override val allDomains: List<AllDomainItem>,
                    val filtered: List<AllDomainItem>
                ) : Loaded()
            }
        }
        object Empty : UiState()
        object Error : UiState()
    }
}

sealed class DomainCardUiState {
    object Initial : DomainCardUiState()
    data class Loaded(
        val domain: String,
        val title: String,
        val detailUrl: String?,
        val statusUiState: StatusRowUiState,
    ) : DomainCardUiState()

    companion object {
        @Composable
        fun fromDomain(domain: AllDomainItem) = Loaded(
            domain = domain.domain,
            title = domain.blogName,
            detailUrl = domain.getDomainDetailsUrl(),
            statusUiState = StatusRowUiState.Loaded(
                indicatorColor = domain.domainStatus.indicatorColor,
                statusText = domain.domainStatus.statusText,
                textColor = domain.domainStatus.textColor,
                isBold = domain.domainStatus.isBold,
                expiry = domain.expiry?.toLocalDateOrNull(),
            )
        )
    }
}

sealed class StatusRowUiState {
    object Initial : StatusRowUiState()
    data class Loaded(
        val indicatorColor: Color,
        val statusText: String,
        val textColor: Color,
        val isBold: Boolean = false,
        val expiry: LocalDate?,
    ) : StatusRowUiState()
}

// `AllDomainItem.expiry` is an unvalidated API string in "YYYY-MM-DD" form.
private fun String.toLocalDateOrNull() =
    runCatching { LocalDate.parse(this) }.getOrNull()

val DomainListItemStatus.indicatorColor
    @Composable
    get() = when (statusType) {
        is DomainListItemStatusType.Success,
        is DomainListItemStatusType.Premium ->
            MaterialTheme.colorScheme.success
        is DomainListItemStatusType.Neutral ->
            MaterialTheme.colorScheme.neutral
        is DomainListItemStatusType.Alert ->
            MaterialTheme.colorScheme.error
        is DomainListItemStatusType.Warning ->
            MaterialTheme.colorScheme.warning
        is DomainListItemStatusType.Error,
        is DomainListItemStatusType.Other ->
            MaterialTheme.colorScheme.error
    }

val DomainListItemStatus.statusText
    @Composable
    get() = label.ifEmpty { stringResource(id = R.string.error) }

val DomainListItemStatus.textColor
    @Composable
    get() = when (statusType) {
        is DomainListItemStatusType.Error,
        is DomainListItemStatusType.Other ->
            MaterialTheme.colorScheme.error
        else -> LocalTextStyle.current.color
    }

val DomainListItemStatus.isBold
    @Composable
    get() = when (statusType) {
        is DomainListItemStatusType.Error,
        is DomainListItemStatusType.Other -> true
        else -> false
    }
