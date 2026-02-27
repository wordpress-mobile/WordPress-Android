package org.wordpress.android.ui.newstats.subscribers.subscribersgraph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.SubscribersGraphResult
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
@Suppress("TooGenericExceptionCaught")
class SubscribersGraphViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<SubscribersGraphUiState>(
            SubscribersGraphUiState.Loading
        )
    val uiState: StateFlow<SubscribersGraphUiState> =
        _uiState.asStateFlow()

    private val _selectedTab =
        MutableStateFlow(SubscribersGraphTab.DAYS)
    val selectedTab: StateFlow<SubscribersGraphTab> =
        _selectedTab.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> =
        _isRefreshing.asStateFlow()

    private var isLoading = false
    private var isLoadedSuccessfully = false

    fun loadDataIfNeeded() {
        if (isLoadedSuccessfully || isLoading) return
        isLoading = true
        loadData()
    }

    fun refresh() {
        val site =
            selectedSiteRepository.getSelectedSite()
                ?: return
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                loadDataInternal(site.siteId)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadData() {
        val site =
            selectedSiteRepository.getSelectedSite()
        if (site == null) {
            isLoading = false
            _uiState.value = SubscribersGraphUiState
                .Error(message = "No site selected")
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            isLoading = false
            _uiState.value = SubscribersGraphUiState
                .Error(message = "Not authenticated")
            return
        }

        statsRepository.init(accessToken)
        _uiState.value = SubscribersGraphUiState.Loading

        viewModelScope.launch {
            try {
                loadDataInternal(site.siteId)
            } finally {
                isLoading = false
            }
        }
    }

    fun onTabSelected(tab: SubscribersGraphTab) {
        if (tab == _selectedTab.value) return
        _selectedTab.value = tab
        isLoadedSuccessfully = false
        loadData()
    }

    private suspend fun loadDataInternal(siteId: Long) {
        try {
            val tab = _selectedTab.value
            val today = LocalDate.now()
            val dateStr = today.format(
                DateTimeFormatter.ISO_LOCAL_DATE
            )
            when (
                val result = statsRepository
                    .fetchSubscribersGraph(
                        siteId,
                        unit = tab.unit,
                        quantity = tab.quantity,
                        date = dateStr
                    )
            ) {
                is SubscribersGraphResult.Success -> {
                    isLoadedSuccessfully = true
                    val sorted = result.dataPoints
                        .sortedBy { it.date }
                    _uiState.value =
                        SubscribersGraphUiState.Loaded(
                            dataPoints =
                                sorted.map {
                                    GraphDataPoint(
                                        label = formatLabel(
                                            it.date, tab
                                        ),
                                        count = it.count
                                    )
                                },
                            selectedTab = tab
                        )
                }
                is SubscribersGraphResult.Error -> {
                    _uiState.value =
                        SubscribersGraphUiState.Error(
                            message = resourceProvider
                                .getString(
                                    result.messageResId
                                ),
                            isAuthError =
                                result.isAuthError
                        )
                }
            }
        } catch (e: Exception) {
            _uiState.value =
                SubscribersGraphUiState.Error(
                    message = e.message
                        ?: "Unknown error"
                )
        }
    }

    @Suppress("MagicNumber")
    private fun formatLabel(
        dateStr: String,
        tab: SubscribersGraphTab
    ): String {
        return try {
            val date = LocalDate.parse(dateStr)
            when (tab) {
                SubscribersGraphTab.DAYS -> {
                    val fmt = DateTimeFormatter.ofPattern(
                        "MMM d", Locale.getDefault()
                    )
                    date.format(fmt)
                }
                SubscribersGraphTab.WEEKS -> {
                    val fmt = DateTimeFormatter.ofPattern(
                        "MMM d", Locale.getDefault()
                    )
                    date.format(fmt)
                }
                SubscribersGraphTab.MONTHS -> {
                    val fmt = DateTimeFormatter.ofPattern(
                        "MMM", Locale.getDefault()
                    )
                    date.format(fmt)
                }
                SubscribersGraphTab.YEARS -> {
                    val fmt = DateTimeFormatter.ofPattern(
                        "yyyy", Locale.getDefault()
                    )
                    date.format(fmt)
                }
            }
        } catch (_: Exception) {
            dateStr
        }
    }
}
