package org.wordpress.android.ui.newstats.mostviewed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.MostViewedItemData
import org.wordpress.android.ui.newstats.repository.MostViewedResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import kotlin.math.abs
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

private const val MONTH_ABBREVIATION_LENGTH = 3

@HiltViewModel
class MostViewedViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<MostViewedCardUiState>(MostViewedCardUiState.Loading)
    val uiState: StateFlow<MostViewedCardUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var currentDataSource: MostViewedDataSource = MostViewedDataSource.POSTS_AND_PAGES
    private var currentPeriod: StatsPeriod = StatsPeriod.Last7Days

    private var allItems: List<MostViewedDetailItem> = emptyList()
    private var cachedTotalViews: Long = 0L
    private var cachedTotalViewsChange: Long = 0L
    private var cachedTotalViewsChangePercent: Double = 0.0

    init {
        loadData()
    }

    fun onDataSourceChanged(dataSource: MostViewedDataSource) {
        if (dataSource == currentDataSource) return
        currentDataSource = dataSource
        loadData()
    }

    fun onPeriodChanged(period: StatsPeriod) {
        if (period == currentPeriod) return
        currentPeriod = period
        loadData()
    }

    fun refresh() {
        val site = selectedSiteRepository.getSelectedSite() ?: return
        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) return

        statsRepository.init(accessToken)
        viewModelScope.launch {
            _isRefreshing.value = true
            loadDataInternal(site.siteId)
            _isRefreshing.value = false
        }
    }

    fun onRetry() {
        loadData()
    }

    fun getDetailData(): MostViewedDetailData {
        return MostViewedDetailData(
            dataSource = currentDataSource,
            items = allItems,
            totalViews = cachedTotalViews,
            totalViewsChange = cachedTotalViewsChange,
            totalViewsChangePercent = cachedTotalViewsChangePercent,
            dateRange = currentPeriod.toDateRangeString(resourceProvider)
        )
    }

    private fun loadData() {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            _uiState.value = MostViewedCardUiState.Error(
                message = resourceProvider.getString(R.string.stats_todays_stats_no_site_selected)
            )
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            _uiState.value = MostViewedCardUiState.Error(
                message = resourceProvider.getString(R.string.stats_todays_stats_failed_to_load)
            )
            return
        }

        statsRepository.init(accessToken)
        _uiState.value = MostViewedCardUiState.Loading

        viewModelScope.launch {
            loadDataInternal(site.siteId)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadDataInternal(siteId: Long) {
        try {
            val result = statsRepository.fetchMostViewed(siteId, currentPeriod, currentDataSource)

            when (result) {
                is MostViewedResult.Success -> {
                    cachedTotalViews = result.totalViews
                    cachedTotalViewsChange = result.totalViewsChange
                    cachedTotalViewsChangePercent = result.totalViewsChangePercent

                    allItems = result.items.map { item ->
                        MostViewedDetailItem(
                            id = item.id,
                            title = item.title,
                            views = item.views,
                            change = item.toMostViewedChange()
                        )
                    }
                    _uiState.value = MostViewedCardUiState.Loaded(
                        selectedDataSource = currentDataSource,
                        items = allItems.take(CARD_MAX_ITEMS).mapIndexed { index, item ->
                            MostViewedItem(
                                id = item.id,
                                title = item.title,
                                views = item.views,
                                change = item.change,
                                isHighlighted = index == 0
                            )
                        }
                    )
                }
                is MostViewedResult.Error -> {
                    _uiState.value = MostViewedCardUiState.Error(
                        message = resourceProvider.getString(R.string.stats_todays_stats_failed_to_load)
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.value = MostViewedCardUiState.Error(
                message = e.message
                    ?: resourceProvider.getString(R.string.stats_todays_stats_unknown_error)
            )
        }
    }

    companion object {
        private const val CARD_MAX_ITEMS = 10
    }
}

data class MostViewedDetailData(
    val dataSource: MostViewedDataSource,
    val items: List<MostViewedDetailItem>,
    val totalViews: Long,
    val totalViewsChange: Long,
    val totalViewsChangePercent: Double,
    val dateRange: String
)

private fun MostViewedItemData.toMostViewedChange(): MostViewedChange {
    return when {
        viewsChange > 0 -> MostViewedChange.Positive(viewsChange, abs(viewsChangePercent))
        viewsChange < 0 -> MostViewedChange.Negative(abs(viewsChange), abs(viewsChangePercent))
        else -> MostViewedChange.NoChange
    }
}

private fun StatsPeriod.toDateRangeString(resourceProvider: ResourceProvider): String {
    return when (this) {
        is StatsPeriod.Today -> resourceProvider.getString(R.string.stats_period_today)
        is StatsPeriod.Last7Days -> resourceProvider.getString(R.string.stats_period_last_7_days)
        is StatsPeriod.Last30Days -> resourceProvider.getString(R.string.stats_period_last_30_days)
        is StatsPeriod.Last6Months -> resourceProvider.getString(R.string.stats_period_last_6_months)
        is StatsPeriod.Last12Months -> resourceProvider.getString(R.string.stats_period_last_12_months)
        is StatsPeriod.Custom -> "${startDate.dayOfMonth}-${endDate.dayOfMonth} ${
            endDate.month.name.take(MONTH_ABBREVIATION_LENGTH).lowercase().replaceFirstChar { it.uppercase() }
        }"
    }
}
