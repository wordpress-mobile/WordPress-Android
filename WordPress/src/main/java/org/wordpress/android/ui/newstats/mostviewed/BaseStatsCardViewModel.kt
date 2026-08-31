package org.wordpress.android.ui.newstats.mostviewed

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider

private const val CARD_MAX_ITEMS = 10

/**
 * Abstract base class for stats card ViewModels that share
 * the same loading/refresh/period/error handling pattern.
 *
 * Subclasses only need to provide:
 * - [logTag]: a label for error logging
 * - [fetchStats]: the suspend function that calls the
 *   repository and maps the result to [StatsCardFetchResult]
 */
abstract class BaseStatsCardViewModel(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    protected val statsRepository: StatsRepository,
    protected val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<MostViewedCardUiState>(
        MostViewedCardUiState.Loading
    )
    val uiState: StateFlow<MostViewedCardUiState> =
        _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> =
        _isRefreshing.asStateFlow()

    private var currentPeriod: StatsPeriod = StatsPeriod.Last7Days
    private var loadingPeriod: StatsPeriod? = null
    private var loadedPeriod: StatsPeriod? = null
    private var fetchJob: Job? = null
    protected abstract val logTag: String

    protected abstract suspend fun fetchStats(
        siteId: Long,
        period: StatsPeriod
    ): StatsCardFetchResult

    fun loadData() {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            loadingPeriod = null
            _uiState.value = MostViewedCardUiState.Error(
                resourceProvider.getString(
                    R.string.stats_error_no_site
                )
            )
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            loadingPeriod = null
            _uiState.value = MostViewedCardUiState.Error(
                resourceProvider.getString(
                    R.string.stats_error_api
                )
            )
            return
        }

        statsRepository.init(accessToken)
        _uiState.value = MostViewedCardUiState.Loading

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            try {
                fetchAndProcess(site)
            } finally {
                loadingPeriod = null
            }
        }
    }

    fun refresh() {
        val site =
            selectedSiteRepository.getSelectedSite() ?: return
        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) return

        statsRepository.init(accessToken)
        loadingPeriod = currentPeriod
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            try {
                _isRefreshing.value = true
                fetchAndProcess(site)
            } finally {
                _isRefreshing.value = false
                loadingPeriod = null
            }
        }
    }

    fun onRetry() {
        loadData()
    }

    fun getAdminUrl(): String? =
        selectedSiteRepository.getSelectedSite()?.adminUrl

    fun onPeriodChanged(period: StatsPeriod) {
        if (loadedPeriod == period || loadingPeriod == period) {
            return
        }
        loadingPeriod = period
        currentPeriod = period
        loadData()
    }

    /**
     * The period currently selected for this card. The detail screen self-fetches its own (unbounded)
     * data for this period rather than receiving the full list via the Intent.
     */
    fun getCurrentPeriod(): StatsPeriod = currentPeriod

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchAndProcess(site: SiteModel) {
        val siteId = site.siteId

        try {
            when (
                val result = fetchStats(siteId, currentPeriod)
            ) {
                is StatsCardFetchResult.Success -> {
                    loadedPeriod = currentPeriod

                    if (result.items.isEmpty()) {
                        _uiState.value =
                            MostViewedCardUiState.Loaded(
                                items = emptyList(),
                                maxViewsForBar = 0
                            )
                    } else {
                        val cardItems = result.items
                            .take(CARD_MAX_ITEMS)
                        val maxForBar =
                            cardItems.firstOrNull()?.views ?: 0L

                        _uiState.value =
                            MostViewedCardUiState.Loaded(
                                items = cardItems.map {
                                    it.toMostViewedItem()
                                },
                                maxViewsForBar = maxForBar
                            )
                    }
                }
                is StatsCardFetchResult.Error -> {
                    _uiState.value = MostViewedCardUiState.Error(
                        message = resourceProvider.getString(
                            result.messageResId
                        ),
                        isAuthError = result.isAuthError,
                        isNotAvailable = result.isNotAvailable
                    )
                }
            }
        } catch (e: Exception) {
            AppLog.e(
                AppLog.T.STATS,
                "Error fetching $logTag",
                e
            )
            _uiState.value = MostViewedCardUiState.Error(
                resourceProvider.getString(
                    R.string.stats_error_unknown
                )
            )
        }
    }

    private fun MostViewedDetailItem.toMostViewedItem() =
        MostViewedItem(
            id = id,
            title = title,
            views = views,
            change = change,
            children = children,
            url = url
        )
}

sealed class StatsCardFetchResult {
    data class Success(
        val items: List<MostViewedDetailItem>,
        val totalValue: Long,
        val totalValueChange: Long,
        val totalValueChangePercent: Double
    ) : StatsCardFetchResult()

    data class Error(
        @StringRes val messageResId: Int,
        val isAuthError: Boolean = false,
        val isNotAvailable: Boolean = false
    ) : StatsCardFetchResult()
}
